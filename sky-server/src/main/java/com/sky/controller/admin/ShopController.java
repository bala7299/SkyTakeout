package com.sky.controller.admin;


import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    private static final String KEY = "shop_status";

    /**
     * 设置店铺营业状态
     *
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺营业状态为：{}", status == 1 ? "营业中" : "店铺打烊");
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    /**
     * 查询店铺营业状态
     *
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("查询店铺营业状态")
    public Result<Integer> getStatus() {
        Object statusObj = (Integer) redisTemplate.opsForValue().get(KEY);
        Integer shopStatus = (statusObj != null) ? (Integer) statusObj : 0;
        // 优化点：如果 Redis 没值（比如被删了），给一个默认值 0（打烊）
        // 这样前端会显示“打烊”，而不是直接 500 崩溃
        if (statusObj == null) {
            log.warn("警告：Redis 中未找到营业状态缓存，已自动降级为【打烊】状态。");
        }
        log.info("当前店铺营业状态为：{}", shopStatus == 1 ? "营业中" : "店铺打烊");
        return Result.success(shopStatus);
    }

}
