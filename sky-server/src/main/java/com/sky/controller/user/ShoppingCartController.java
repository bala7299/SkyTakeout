package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api(tags = "C端-购物车相关接口")
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车：{}", shoppingCartDTO);
        shoppingCartService.add(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 查看购物车
     *
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<ShoppingCart>> list() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> list = shoppingCartService.list(shoppingCart);
        return Result.success(list);
    }

    /**
     * 清空购物车
     *
     * @return
     */
    @ApiOperation("清空购物车")
    @DeleteMapping("/clean")
    public Result delete() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        shoppingCartService.deleteByUserId(shoppingCart);
        return Result.success();
    }

    @PostMapping("/sub")
    @ApiOperation("删除购物车中单个商品")
    public Result deleteSinglecart(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("删除单个购物车商品：{}", shoppingCartDTO);
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId).dishId(shoppingCartDTO.getDishId()).setmealId(shoppingCartDTO.getSetmealId())
                .build();
        shoppingCartService.deleteSinglecart(shoppingCart);
        return Result.success();
    }
}
