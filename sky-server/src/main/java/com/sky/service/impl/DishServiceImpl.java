package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.AIService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private AIService aiService;

    /**
     * 新增菜品
     *
     * @param dishDTO
     */
    @Transactional
    public void saveDishWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setStatus(StatusConstant.DISABLE);
        
        // 提取口味标签
        if (dishDTO.getDescription() != null) {
            String flavorTag = aiService.extractFlavorTag(dishDTO.getName(), dishDTO.getDescription());
            dish.setFlavorTag(flavorTag);
        }
        
        //向菜表加入一条数据
        dishMapper.insert(dish);
        List<DishFlavor> dishFlavorList = dishDTO.getFlavors();
        //主键返回获取id
        Long dishid = dish.getId();
        //向口味表加入n条数据
        if (dishFlavorList != null && dishFlavorList.size() > 0) {
            for (DishFlavor dishFlavor : dishFlavorList) {
                dishFlavor.setDishId(dishid);
            }
            dishFlavorMapper.insertBatch(dishFlavorList);
        }
        
        // 菜品向量入库
        if (dish.getFlavorTag() != null) {
            String vectorId = "D_" + dishid;
            aiService.syncDishToVectorDB(vectorId, dish.getName(), dish.getFlavorTag());
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // select * from category limit 0,10
        // 开始类别分页查询
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        Long total = page.getTotal();
        List<DishVO> dishlist = page.getResult();
        return new PageResult(total, dishlist);
    }

    /**
     * 根据id删除菜品
     *
     * @param ids
     */
    public void deleteById(List<Long> ids) {
        //delete from dish where id = #{id}
        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            //查看菜品是否处于起售状态
            if (dish.getStatus() != 0) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //查看菜品是否与套餐关联
        List<Long> setMealId = setmealMapper.getSetmealIdByDishId(ids);
        if (setMealId != null && setMealId.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //批量删除菜品与口味
        dishMapper.deleteById(ids);
        //并且还要删除口味表中对应被删除菜品的口味数据
        dishFlavorMapper.deleteByDishId(ids);
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    public DishVO getById(Long id) {
        Dish dish = dishMapper.selectById(id);
        List<DishFlavor> dishFlavor = dishFlavorMapper.selectByDishId(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavor);
        return dishVO;
    }


    /**
     * 修改菜品
     *
     * @param dishDTO
     */
    public void updateDish(DishDTO dishDTO) {
        //DishDTO里面的数据有多余（口味数据）  口味数据不属于dish表 因此要将属于dish表的数据提取出来再更新
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        
        // 提取口味标签
        if (dishDTO.getDescription() != null) {
            String flavorTag = aiService.extractFlavorTag(dishDTO.getName(), dishDTO.getDescription());
            dish.setFlavorTag(flavorTag);
        }
        
        dishMapper.update(dish);
        //然后再批量修改口味表    先删再插
        List<DishFlavor> dishFlavorList = dishDTO.getFlavors();
        List<Long> ids = List.of(dishDTO.getId());
        dishFlavorMapper.deleteByDishId(ids);
        if (dishFlavorList != null && dishFlavorList.size() > 0) {
            for (DishFlavor dishFlavor : dishFlavorList) {
                dishFlavor.setDishId(dishDTO.getId());
            }
            dishFlavorMapper.insertBatch(dishFlavorList);
        }
        
        // 菜品向量入库
        if (dish.getFlavorTag() != null) {
            String vectorId = "D_" + dishDTO.getId();
            aiService.syncDishToVectorDB(vectorId, dishDTO.getName(), dish.getFlavorTag());
        }
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 修改菜品起售状态
     *
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
    }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

}
