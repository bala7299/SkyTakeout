package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     *
     * @param openid
     * @return
     */
    User getByOpenId(String openid);

    /**
     * 插入用户
     * @param user
     */
    void insert(User user);

    /**
     * 根据用户id查询用户
     * @param userId
     * @return
     */
    User getById(Long userId);

    /**
     * 更新用户信息
     * @param user
     */
    void update(User user);

    /**
     * 根据日期查询用户数量
     * @param map
     * @return
     */
    Integer countUserByDate(Map map);

    /**
     * 查询所有未设置口味画像的用户
     * @return 未设置画像的用户列表
     */
    @Select("SELECT * FROM user WHERE flavor_profile IS NULL OR flavor_profile = ''")
    List<User> getUsersWithoutProfile();
}
