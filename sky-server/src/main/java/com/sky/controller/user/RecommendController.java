package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.RecommendService;
import com.sky.vo.AIRecommendVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user/recommend")
@Api(tags = "C端-智能推荐相关接口")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 获取AI推荐
     * @return AI推荐结果
     */
    @ApiOperation("获取AI智能推荐")
    @GetMapping("/ai")
    public Result<AIRecommendVO> getAIRecommendation() {
        AIRecommendVO aiRecommendVO = recommendService.getAIRecommendation();
        return Result.success(aiRecommendVO);
    }
}