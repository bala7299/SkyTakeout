package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("userShopController")
@RequestMapping("/user/shop")
@Tag(name = "C端-店铺相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    private static final String KEY = "shop_status";

    /**
     * 查询店铺营业状态
     *
     * @return
     */
    @GetMapping("/status")
    @Operation(summary = "查询店铺营业状态")
    public Result<Integer> getStatus() {
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("当前店铺营业状态为：{}", shopStatus == 1 ? "营业中" : "店铺打烊");
        return Result.success(shopStatus);
    }

}
