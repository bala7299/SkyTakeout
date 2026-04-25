package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.dto.AiChatDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.utils.HttpClientUtil;
import com.sky.service.AIService;
import com.sky.vo.AiChatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private static final String AI_BASE_URL = "http://localhost:8000";

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ChatClient.Builder builder;

    @Autowired
    private ChatMemory chatMemory;


    private ChatClient chatClient;

    // Spring AI的ChatClient初始化，理解为client的底层设定，后面的prompt()是不同方法中具体的对话逻辑
    // @PostConstruct的意思是，在Bean初始化完成后，调用init方法，初始化ChatClient
    @PostConstruct
    public void init() {
        this.chatClient = builder
                .defaultSystem("你是一个苍穹外卖的温柔客服蘑菇酱，你会用可爱的语气和颜文字回答问题。如果查询订单失败，请温柔地引导用户提供正确的订单号。")
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    @Override
    public String extractFlavorTag(String dishName, String description) {
        String url = AI_BASE_URL + "/ai-flavor-extract";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("dish_name", dishName);
        paramMap.put("description", description);

        try {
            String response = HttpClientUtil.doPost4Json(url, paramMap);
            JSONObject jsonObject = JSONObject.parseObject(response);
            return jsonObject.getString("flavor_tag");
        } catch (Exception e) {
            log.error("提取口味标签失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void syncDishToVectorDB(String vectorId, String dishName, String flavorTag) {
        String url = AI_BASE_URL + "/ai-ingest";

        try {
            // 使用JSONObject发送请求，确保字段名与Python端IngestRequest一致
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("dish_id", vectorId);
            jsonObject.put("dish_name", dishName);
            jsonObject.put("flavor_tag", flavorTag);

            HttpClientUtil.doPostJsonBody(url, jsonObject.toString());
        } catch (Exception e) {
            log.error("菜品向量入库失败: {}", e.getMessage());
        }
    }

    public String updateFlavorProfile(String currentProfile, List<String> dishNames) {
        // 1. 路径必须和 Python 保持完全一致
        String url = AI_BASE_URL + "/ai-update-profile";

        try {
            // 2. 直接用 JSONObject，干净利落
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("current_profile", currentProfile);
            jsonObject.put("new_dishes", dishNames);

            // 3. 发送 POST 请求
            String response = HttpClientUtil.doPostJsonBody(url, jsonObject.toString());
            JSONObject result = JSONObject.parseObject(response);

            // 4. 解析 Python 返回的 {"status": "success", "new_profile": "..."}
            if ("success".equals(result.getString("status"))) {
                return result.getString("new_profile");
            }
            log.warn("AI未能成功更新画像，返回结果: {}", response);
            return currentProfile;
        } catch (Exception e) {
            log.error("更新用户口味画像失败: {}", e.getMessage());
            return currentProfile; // 发生异常时，原样返回老画像，不破坏原有数据
        }
    }

    @Override
    public Map<String, Object> getAIRecommendation(String userProfile) {
        String url = AI_BASE_URL + "/ai-recommend";

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_profile", userProfile);

            String response = HttpClientUtil.doPostJsonBody(url, jsonObject.toString());
            JSONObject result = JSONObject.parseObject(response);

            if ("success".equals(result.getString("status"))) {
                Map<String, Object> recommendationResult = new HashMap<>();
                recommendationResult.put("recommendation", result.getString("recommendation"));
                recommendationResult.put("matched_dish_ids", result.getJSONArray("matched_dish_ids"));
                return recommendationResult;
            }

            log.warn("AI推荐失败，返回结果: {}", response);
            return createDefaultRecommendation();
        } catch (Exception e) {
            log.error("获取AI推荐失败: {}", e.getMessage());
            return createDefaultRecommendation();
        }
    }

    private Map<String, Object> createDefaultRecommendation() {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("recommendation", "暂时无法提供个性化推荐，请稍后再试。");
        defaultResult.put("matched_dish_ids", new ArrayList<>());
        return defaultResult;
    }

    @Override
    public void initializeVectorDB() {
        log.info("开始初始化向量数据库...");

        int dishCount = 0;
        int setmealCount = 0;

        try {
            // 1. 查询所有未标记口味标签的菜品（SQL 层面过滤）
            List<Dish> dishes = dishMapper.getUnlabeledDishes();
            if (dishes != null && !dishes.isEmpty()) {
                for (Dish dish : dishes) {
                    try {
                        // 调用 AI 提取口味标签
                        String desc = dish.getDescription() != null ? dish.getDescription() : "这道菜很神秘，暂无描述";
                        String flavorTag = extractFlavorTag(dish.getName(), desc);
                        if (flavorTag != null && !flavorTag.trim().isEmpty()) {
                            // 更新 MySQL 数据库
                            Dish updateDish = new Dish();
                            updateDish.setId(dish.getId());
                            updateDish.setFlavorTag(flavorTag);
                            dishMapper.update(updateDish);

                            // 同步到向量数据库（带上 D_ 前缀）
                            String vectorId = "D_" + dish.getId();
                            syncDishToVectorDB(vectorId, dish.getName(), flavorTag);

                            dishCount++;
                            log.info("已处理菜品：{} (ID: {}), 口味标签：{}", dish.getName(), dish.getId(), flavorTag);

                            // 延迟 200ms，防止 API 频率过高
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.warn("处理菜品时被中断");
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理菜品失败：{}, 错误信息：{}", dish.getName(), e.getMessage());
                    }
                }
            }

            // 2. 查询所有未标记口味标签的套餐（SQL 层面过滤）
            List<Setmeal> setmeals = setmealMapper.getUnlabeledSetmeals();
            if (setmeals != null && !setmeals.isEmpty()) {
                for (Setmeal setmeal : setmeals) {
                    try {
                        // 调用 AI 提取口味标签
                        String desc = setmeal.getDescription() != null ? setmeal.getDescription() : "这份套餐很神秘，暂无描述";
                        String flavorTag = extractFlavorTag(setmeal.getName(), desc);
                        if (flavorTag != null && !flavorTag.trim().isEmpty()) {
                            // 更新 MySQL 数据库
                            Setmeal updateSetmeal = new Setmeal();
                            updateSetmeal.setId(setmeal.getId());
                            updateSetmeal.setFlavorTag(flavorTag);
                            setmealMapper.update(updateSetmeal);

                            // 同步到向量数据库（带上 S_ 前缀）
                            String vectorId = "S_" + setmeal.getId();
                            syncDishToVectorDB(vectorId, setmeal.getName(), flavorTag);

                            setmealCount++;
                            log.info("已处理套餐：{} (ID: {}), 口味标签：{}", setmeal.getName(), setmeal.getId(), flavorTag);

                            // 延迟 200ms，防止 API 频率过高
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.warn("处理套餐时被中断");
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理套餐失败：{}, 错误信息：{}", setmeal.getName(), e.getMessage());
                    }
                }
            }

            log.info("向量数据库初始化完成！共处理菜品 {} 个，套餐 {} 个", dishCount, setmealCount);

        } catch (Exception e) {
            log.error("初始化向量数据库失败：{}", e.getMessage());
        }
    }

    @Override
    public void initializeUserProfileDB() {
        log.info("开始初始化用户画像数据库...");

        int userCount = 0;
        int successCount = 0;

        try {
            // 1. 获取所有未设置口味画像的用户
            List<User> users = userMapper.getUsersWithoutProfile();
            if (users == null || users.isEmpty()) {
                log.info("没有需要初始化画像的用户");
                return;
            }

            userCount = users.size();
            log.info("共找到 {} 个需要初始化画像的用户", userCount);

            // 2. 针对每个用户进行历史订单追溯
            for (User user : users) {
                try {
                    Long userId = user.getId();

                    // 查询该用户所有已完成的订单（status = 5）
                    List<Long> completedOrderIds = orderMapper.getCompletedOrderIdsByUserId(userId);
                    if (completedOrderIds == null || completedOrderIds.isEmpty()) {
                        log.info("用户 {} 没有历史订单，设为默认画像", userId);
                        // 没有历史订单，设为默认值
                        User updateUser = new User();
                        updateUser.setId(userId);
                        updateUser.setFlavorProfile("该用户较内敛，尚未展示口味偏好");
                        updateUser.setFlavorUpdateTime(LocalDateTime.now());
                        userMapper.update(updateUser);
                        successCount++;
                        continue;
                    }

                    // 提取所有消费过的菜品名称，并去重
                    Set<String> allDishNamesSet = new HashSet<>();
                    for (Long orderId : completedOrderIds) {
                        List<String> dishNames = orderDetailMapper.getDishNamesByOrderId(orderId);
                        if (dishNames != null && !dishNames.isEmpty()) {
                            allDishNamesSet.addAll(dishNames);
                        }
                    }

                    List<String> dishNames = new ArrayList<>(allDishNamesSet);
                    log.info("正在为用户 {} 初始化画像，历史消费菜品：{}", userId, dishNames);

                    // 3. 调用 AI 生成初始画像
                    String currentProfile = "这是该用户的历史消费记录，请据此生成初始画像";
                    String newProfile = updateFlavorProfile(currentProfile, dishNames);

                    if (newProfile != null && !newProfile.trim().isEmpty()) {
                        // 4. 数据库回写
                        User updateUser = new User();
                        updateUser.setId(userId);
                        updateUser.setFlavorProfile(newProfile);
                        updateUser.setFlavorUpdateTime(LocalDateTime.now());
                        userMapper.update(updateUser);

                        successCount++;
                        log.info("用户 {} 画像初始化成功：{}", userId, newProfile);
                    } else {
                        log.warn("用户 {} 画像生成失败，使用默认值", userId);
                        User updateUser = new User();
                        updateUser.setId(userId);
                        updateUser.setFlavorProfile("该用户较内敛，尚未展示口味偏好");
                        updateUser.setFlavorUpdateTime(LocalDateTime.now());
                        userMapper.update(updateUser);
                        successCount++;
                    }

                    // 延迟 300ms，防止 API 频率过高
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("处理用户画像时被中断");
                    }

                } catch (Exception e) {
                    log.error("处理用户 {} 画像失败，错误信息：{}", user.getId(), e.getMessage());
                }
            }

            log.info("用户画像数据库初始化完成！共处理 {} 个用户，成功 {} 个", userCount, successCount);

        } catch (Exception e) {
            log.error("初始化用户画像数据库失败：{}", e.getMessage());
        }
    }

    @Override
    public AiChatVO chat(AiChatDTO aiChatDTO) {
        try {
            log.info("接收到用户AI客服消息: {}", aiChatDTO.getMessage());
            String response = chatClient.prompt()
                    .user(aiChatDTO.getMessage())
                    .functions("orderInfoFunction", "cancelOrderFunction", "recommendByTasteFunction", "searchDishFunction", "getDishReviewsFunction", "reOrderFunction")
                    .call()
                    .content();
            log.info("AI客服回复成功");
            return AiChatVO.builder().reply(response).build();
        } catch (Exception e) {
            log.error("AI客服对话失败: {}", e.getMessage());
            return AiChatVO.builder().reply("抱歉，AI客服暂时无法响应，请稍后再试。").build();
        }
    }
}