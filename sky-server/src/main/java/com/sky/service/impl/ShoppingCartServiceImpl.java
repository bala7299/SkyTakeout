package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增购物车
     *
     * @param shoppingCartDTO
     */
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //先判断购物车里有没有这个数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart cart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, cart);
        cart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(cart);
        //如果有就数量加1
        if (list.size() > 0 && list != null) {
            ShoppingCart shoppingCart = list.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(shoppingCart);
            return;
        }
        //没有则新加数据  判断添加的是菜品还是套餐
        Long dishId = shoppingCartDTO.getDishId();
        if (dishId != null) {
            Dish dish = dishMapper.selectById(dishId);
            cart.setAmount(dish.getPrice());
            cart.setName(dish.getName());
            cart.setImage(dish.getImage());
            cart.setDishId(dishId);

        } else {
            Long setmealId = shoppingCartDTO.getSetmealId();
            Setmeal setmeal = setmealMapper.getById(setmealId);
            cart.setAmount(setmeal.getPrice());
            cart.setName(setmeal.getName());
            cart.setImage(setmeal.getImage());
        }
        cart.setCreateTime(LocalDateTime.now());
        cart.setNumber(1);
        shoppingCartMapper.insert(cart);
    }


    /**
     * 查看购物车
     *
     * @param shoppingCart
     * @return
     */
    public List<ShoppingCart> list(ShoppingCart shoppingCart) {
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        return cartList;
    }


    /**
     * 清空购物车
     *
     * @param shoppingCart
     */
    public void deleteByUserId(ShoppingCart shoppingCart) {
        shoppingCartMapper.deleteByUserId(shoppingCart);
    }


    /**
     * 删除单个购物车商品
     *
     * @param shoppingCart
     */
    public void deleteSinglecart(ShoppingCart shoppingCart) {
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            if (cart.getNumber() > 1) {
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(cart);
            } else {
                shoppingCartMapper.deleteByUserId(shoppingCart);
            }
        }
    }
}
