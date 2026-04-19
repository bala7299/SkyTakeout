package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.CommentPageQueryDTO;
import com.sky.dto.CommentSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderCommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/comment")
@Api(tags = "C端-评价接口")
@Slf4j
public class OrderCommentController {

    @Autowired
    private OrderCommentService orderCommentService;

    /**
     * 用户提交评价
     *
     * @param commentSubmitDTO 提交评价数据
     * @return 处理结果
     */
    @PostMapping("/submit")
    @ApiOperation("用户提交评价")
    public Result<String> submit(@RequestBody CommentSubmitDTO commentSubmitDTO) {
        log.info("用户提交评价：{}", commentSubmitDTO);
        orderCommentService.submitComment(commentSubmitDTO);
        return Result.success();
    }

    /**
     * 用户查看历史评价
     *
     * @param commentPageQueryDTO Query 分页与可选筛选（userId 由服务端强制写入）
     * @return 分页结果，列表元素为 CommentVO
     */
    @GetMapping("/history")
    @ApiOperation("用户查看历史评价")
    public Result<PageResult> history(CommentPageQueryDTO commentPageQueryDTO) {
        commentPageQueryDTO.setUserId(BaseContext.getCurrentId());
        log.info("用户查看历史评价：{}", commentPageQueryDTO);
        PageResult pageResult = orderCommentService.pageQuery4User(commentPageQueryDTO);
        return Result.success(pageResult);
    }
}

