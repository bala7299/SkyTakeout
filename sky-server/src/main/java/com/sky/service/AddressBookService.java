package com.sky.service;

import com.sky.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    List<AddressBook> list(AddressBook addressBook);

    void save(AddressBook addressBook);

    AddressBook getById(Long id);

    void update(AddressBook addressBook);

    void setDefault(AddressBook addressBook);

    void deleteById(Long id);

    /**
     * 查询用户默认地址（带缓存）
     *
     * @param userId 用户ID
     * @return 默认地址，无则返回null
     */
    AddressBook getDefaultAddress(Long userId);
}
