package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.AIService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/ai")
@Api(tags = "AI 服务管理接口")
public class AIServiceController {

    @Autowired
    private AIService aiService;

    /**
     * 初始化向量数据库（菜品和套餐）
     * @return 操作结果
     */
    @ApiOperation("初始化向量数据库（菜品和套餐）")
    @GetMapping("/dishInit")
    public Result<String> initVectorDB() {
        log.info("接收到初始化向量数据库请求");
        
        try {
            // 调用 Service 层方法初始化向量数据库
            aiService.initializeVectorDB();
            return Result.success("向量数据库初始化完成");
        } catch (Exception e) {
            log.error("初始化向量数据库失败：{}", e.getMessage());
            return Result.error("初始化失败：" + e.getMessage());
        }
    }

    /**
     * 初始化用户画像数据库
     * @return 操作结果
     */
    @ApiOperation("初始化用户画像数据库")
    @GetMapping("/userInit")
    public Result<String> initUserProfileDB() {
        log.info("接收到初始化用户画像数据库请求");
        
        try {
            // 调用 Service 层方法初始化用户画像数据库
            aiService.initializeUserProfileDB();
            return Result.success("用户画像数据库初始化完成");
        } catch (Exception e) {
            log.error("初始化用户画像数据库失败：{}", e.getMessage());
            return Result.error("初始化失败：" + e.getMessage());
        }
    }
}