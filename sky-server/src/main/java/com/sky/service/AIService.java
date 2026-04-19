package com.sky.service;

import com.sky.dto.AiChatDTO;
import com.sky.vo.AiChatVO;

import java.util.List;
import java.util.Map;

public interface AIService {
    /**
     * 提取口味标签
     * @param dishName 菜品名称
     * @param description 菜品描述
     * @return 口味标签
     */
    String extractFlavorTag(String dishName, String description);

    /**
     * 菜品向量入库
     * @param vectorId 向量数据库 ID（格式：D_123 或 S_456）
     * @param dishName 菜品名称
     * @param flavorTag 口味标签
     */
    void syncDishToVectorDB(String vectorId, String dishName, String flavorTag);

    /**
     * 更新用户口味画像
     * @param oldFlavorProfile 旧口味画像
     * @param dishNames 菜品名称列表
     * @return 新口味画像
     */
    String updateFlavorProfile(String oldFlavorProfile, List<String> dishNames);

    /**
     * 获取 AI 推荐
     * @param userProfile 用户口味画像
     * @return 推荐结果，包含推荐文案和匹配的菜品 ID 列表
     */
    Map<String, Object> getAIRecommendation(String userProfile);

    /**
     * 初始化向量数据库
     * 批量提取菜品和套餐的口味标签，并同步到向量数据库
     */
    void initializeVectorDB();

    /**
     * 初始化用户画像数据库
     * 追溯所有历史用户的消费习惯并生成初始画像
     */
    void initializeUserProfileDB();

    /**
     * AI智能客服对话
     * @param aiChatDTO 用户消息
     * @return AI回复
     */
    AiChatVO chat(AiChatDTO aiChatDTO);
}