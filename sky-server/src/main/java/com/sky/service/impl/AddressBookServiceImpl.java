package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String ADDRESS_CACHE_PREFIX = "user:default_address:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * 条件查询
     *
     * @param addressBook
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);
        return addressBook;
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        addressBookMapper.update(addressBook);
        evictAddressCache(addressBook.getUserId());
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);

        // 清除默认地址缓存
        evictAddressCache(addressBook.getUserId());
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    public void deleteById(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);
        if (addressBook != null) {
            evictAddressCache(addressBook.getUserId());
        }
        addressBookMapper.deleteById(id);
    }

    /**
     * 查询用户默认地址（带缓存）
     *
     * @param userId 用户ID
     * @return 默认地址，无则返回null
     */
    public AddressBook getDefaultAddress(Long userId) {
        String cacheKey = ADDRESS_CACHE_PREFIX + userId;

        // 1. 优先从 Redis 缓存获取
        try {
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                log.info("用户 {} 命中地址缓存", userId);
                return JSON.parseObject(cacheValue, AddressBook.class);
            }
        } catch (Exception e) {
            log.warn("读取地址缓存失败，降级查数据库，用户ID：{}", userId);
        }

        // 2. 缓存未命中，查询数据库
        AddressBook query = new AddressBook();
        query.setUserId(userId);
        query.setIsDefault(1);
        List<AddressBook> list = addressBookMapper.list(query);

        if (list != null && !list.isEmpty()) {
            AddressBook defaultAddress = list.get(0);

            // 3. 写入 Redis 缓存
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(defaultAddress), CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                log.info("用户 {} 默认地址已缓存", userId);
            } catch (Exception e) {
                log.warn("写入地址缓存失败，用户ID：{}", userId);
            }

            return defaultAddress;
        }

        return null;
    }

    /**
     * 清除用户地址缓存
     *
     * @param userId 用户ID
     */
    private void evictAddressCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            String cacheKey = ADDRESS_CACHE_PREFIX + userId;
            stringRedisTemplate.delete(cacheKey);
            log.info("用户 {} 地址缓存已清除", userId);
        } catch (Exception e) {
            log.warn("清除地址缓存失败，用户ID：{}", userId);
        }
    }
}
