package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.mapper.CategoryMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
@Tag(name = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 新增分类
     *
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增分类")
    public Result save(@RequestBody CategoryDTO categoryDTO) {
        log.info("新增分类:{}", categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 启用或禁用分类
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "启用或禁用分类")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用或禁用分类:{},{}", status, id);
        categoryService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据类型查询分类
     *
     * @param type
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根据类型查询分类")
    public Result<List<Category>> getByType(Integer type) {
        List<Category> list = categoryService.getByType(type);
        return Result.success(list);
    }

    /**
     * 修改分类
     *
     * @param categoryDTO
     */
    @PutMapping
    @Operation(summary = "修改分类")
    public Result update(@RequestBody CategoryDTO categoryDTO) {
        categoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 类别分页查询
     *
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "类别分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("类别分页查询");
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id删除分类
     *
     * @param id
     * @return
     */
    @DeleteMapping
    @Operation(summary = "根据id删除分类")
    public Result deleteById(Long id) {
        log.info("根据id删除分类 {}", id);
        categoryService.deleteById(id);
        return Result.success();
    }
}
