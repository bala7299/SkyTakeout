package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/admin/dish")
@Tag(name = "菜品相关接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     *
     * @param dishDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增菜品")
    public Result saveDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveDishWithFlavor(dishDTO);
        //清除redis缓存
        String key = "dish_" + dishDTO.getCategoryId();
        redisTemplate.delete(key);
        return Result.success();
    }

    /**
     * 修改菜品起售状态
     *
     * @param status
     * @param id
     * @return
     */
    @Operation(summary = "修改菜品起售状态")
    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status,Long id) {
        dishService.startOrStop(status, id);
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "菜品分页查询")

    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询");
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id删除菜品
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @Operation(summary = "根据id删除菜品")
    public Result deleteById(@RequestParam List<Long> ids) {
        log.info("根据id删除菜品:{}", ids);
        dishService.deleteById(ids);
        // 清除redis缓存 将所有菜品的缓存都清理掉 所有以dish_开头的key
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询菜品")
    public Result<DishVO> getDishById(@PathVariable Long id) {
        log.info("根据id查询菜品: {}", id);
        DishVO dishvo = dishService.getById(id);
        return Result.success(dishvo);
    }

    @PutMapping
    @Operation(summary = "修改菜品")
    public Result updateDish(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品");
        dishService.updateDish(dishDTO);
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }


    private void cleanCache(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
