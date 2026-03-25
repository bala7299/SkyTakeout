package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    /**
     * 新增购物车
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查看购物车
     *
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     * @param shoppingCart
     */
    void deleteByUserId(ShoppingCart shoppingCart);

    /**
     * 删除单个购物车商品
     * @param shoppingCart
     */
    void deleteSinglecart(ShoppingCart shoppingCart);
}
