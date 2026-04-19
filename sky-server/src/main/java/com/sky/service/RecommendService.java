package com.sky.service;

import com.sky.vo.AIRecommendVO;

/**
 * 智能推荐服务接口
 */
public interface RecommendService {

    /**
     * 获取AI智能推荐
     * @return AI推荐结果
     */
    AIRecommendVO getAIRecommendation();
}