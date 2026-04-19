package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.TopSalesItemDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.AIService;
import com.sky.service.RecommendService;
import com.sky.vo.AIRecommendVO;
import com.sky.vo.RecommendItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private AIService aiService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    public AIRecommendVO getAIRecommendation() {
        try {
            // 获取当前用户ID
            //Long userId = BaseContext.getCurrentId();
            Long userId = 1L;
            // 获取当前用户的口味画像
            User user = userMapper.getById(userId);
            if (user == null) {
                log.warn("用户 {} 不存在", userId);
                return createEmptyDefaultRecommendation();
            }
            
            String userProfile = user.getFlavorProfile();
            if (userProfile == null || userProfile.trim().isEmpty()) {
                log.info("用户 {} 暂无口味画像，使用销量排行榜推荐", userId);
                return createTopSalesRecommendation();
            }
            
            // 调用AI服务获取推荐
            Map<String, Object> aiResult = aiService.getAIRecommendation(userProfile);
            
            // 解析AI返回的菜品ID列表
            List<String> matchedDishIds = (List<String>) aiResult.get("matched_dish_ids");
            String recommendation = (String) aiResult.get("recommendation");
            
            // 构建推荐项列表
            List<RecommendItemVO> recommendItems = new ArrayList<>();
            
            for (String dishId : matchedDishIds) {
                try {
                    RecommendItemVO item = parseDishId(dishId);
                    if (item != null) {
                        recommendItems.add(item);
                    }
                } catch (Exception e) {
                    log.warn("解析菜品ID失败: {}, 错误信息: {}", dishId, e.getMessage());
                }
            }
            
            // 如果AI推荐结果为空，使用销量排行榜兜底
            if (recommendItems.isEmpty()) {
                log.info("AI推荐结果为空，使用销量排行榜兜底");
                return createTopSalesRecommendation();
            }
            
            // 构建返回结果
            AIRecommendVO aiRecommendVO = AIRecommendVO.builder()
                    .recommendation(recommendation)
                    .items(recommendItems)
                    .build();
            
            log.info("用户 {} AI推荐成功，推荐菜品数量: {}", userId, recommendItems.size());
            return aiRecommendVO;
            
        } catch (Exception e) {
            log.error("获取AI推荐失败，用户ID: {}, 错误信息: {}", BaseContext.getCurrentId(), e.getMessage());
            return createTopSalesRecommendation();
        }
    }
    
    /**
     * 解析菜品ID并查询对应的菜品或套餐信息
     * @param dishId 菜品ID（格式：D_123 或 S_456）
     * @return 推荐项VO
     */
    private RecommendItemVO parseDishId(String dishId) {
        if (dishId == null || dishId.trim().isEmpty()) {
            return null;
        }
        
        try {
            if (dishId.startsWith("D_")) {
                // 菜品ID处理
                String idStr = dishId.substring(2);
                Long id = Long.parseLong(idStr);
                
                Dish dish = dishMapper.selectById(id);
                if (dish == null) {
                    log.warn("菜品不存在，ID: {}", id);
                    return null;
                }
                
                return RecommendItemVO.builder()
                        .id(dish.getId())
                        .name(dish.getName())
                        .price(dish.getPrice())
                        .image(dish.getImage())
                        .flavorTag(dish.getFlavorTag())
                        .type(0) // 0表示菜品
                        .build();
                        
            } else if (dishId.startsWith("S_")) {
                // 套餐ID处理
                String idStr = dishId.substring(2);
                Long id = Long.parseLong(idStr);
                
                Setmeal setmeal = setmealMapper.getById(id);
                if (setmeal == null) {
                    log.warn("套餐不存在，ID: {}", id);
                    return null;
                }
                
                return RecommendItemVO.builder()
                        .id(setmeal.getId())
                        .name(setmeal.getName())
                        .price(setmeal.getPrice())
                        .image(setmeal.getImage())
                        .flavorTag(setmeal.getFlavorTag())
                        .type(1) // 1表示套餐
                        .build();
            } else {
                log.warn("无效的菜品ID格式: {}", dishId);
                return null;
            }
        } catch (NumberFormatException e) {
            log.warn("菜品ID格式错误: {}", dishId);
            return null;
        } catch (Exception e) {
            log.error("解析菜品ID异常: {}, 错误信息: {}", dishId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建销量排行榜推荐（冷启动兜底逻辑）
     * @return 销量排行榜推荐结果
     */
    private AIRecommendVO createTopSalesRecommendation() {
        try {
            // 查询销量前5的菜品/套餐
            List<TopSalesItemDTO> topSalesItems = orderDetailMapper.getTopSales(5);
            
            if (topSalesItems == null || topSalesItems.isEmpty()) {
                log.warn("销量排行榜为空");
                return createEmptyDefaultRecommendation();
            }
            
            // 将 TopSalesItemDTO 转换为 RecommendItemVO
            List<RecommendItemVO> recommendItems = new ArrayList<>();
            for (TopSalesItemDTO dto : topSalesItems) {
                RecommendItemVO vo = new RecommendItemVO();
                vo.setName(dto.getName());
                vo.setImage(dto.getImage());
                vo.setPrice(dto.getPrice());
                vo.setFlavorTag(null); // 销量排行榜暂无口味标签
                
                // 转换规则：若 dishId 不为空，则 type=0；若 setmealId 不为空，则 type=1
                if (dto.getDishId() != null) {
                    vo.setId(dto.getDishId());
                    vo.setType(0); // 菜品
                } else if (dto.getSetmealId() != null) {
                    vo.setId(dto.getSetmealId());
                    vo.setType(1); // 套餐
                }
                
                recommendItems.add(vo);
            }
            
            // 硬编码推荐语
            String recommendation = "您还没有建立口味画像哦，为您推荐店里的【人气销量王】，盲点绝不踩雷！";
            
            AIRecommendVO aiRecommendVO = AIRecommendVO.builder()
                    .recommendation(recommendation)
                    .items(recommendItems)
                    .build();
            
            log.info("销量排行榜推荐成功，推荐菜品数量: {}", recommendItems.size());
            return aiRecommendVO;
            
        } catch (Exception e) {
            log.error("获取销量排行榜推荐失败，错误信息: {}", e.getMessage());
            return createEmptyDefaultRecommendation();
        }
    }
    
    /**
     * 创建空默认推荐结果（当销量排行榜也为空时）
     * @return 默认推荐结果
     */
    private AIRecommendVO createEmptyDefaultRecommendation() {
        return AIRecommendVO.builder()
                .recommendation("暂时无法提供个性化推荐，请先完成一次订单来建立您的口味画像。")
                .items(new ArrayList<>())
                .build();
    }
}