package com.sky.controller.admin;

import com.sky.dto.CommentPageQueryDTO;
import com.sky.dto.CommentReplyDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderCommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminOrderCommentController")
@RequestMapping("/admin/comment")
@Tag(name = "管理端-评价接口")
@Slf4j
public class OrderCommentController {

    @Autowired
    private OrderCommentService orderCommentService;

    /**
     * 条件分页查询评价列表
     *
     * @param commentPageQueryDTO 分页与筛选条件
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "条件分页查询评价列表")
    public Result<PageResult> page(CommentPageQueryDTO commentPageQueryDTO) {
        log.info("管理端评价分页查询：{}", commentPageQueryDTO);
        PageResult pageResult = orderCommentService.pageQuery(commentPageQueryDTO);
        log.info(">>> 即将返回给前端的真实数据：{}", pageResult.getRecords());
        return Result.success(pageResult);
    }

    /**
     * 商家回复评价
     *
     * @param commentReplyDTO 评价id与回复内容
     * @return 操作结果
     */
    @PutMapping("/reply")
    @Operation(summary = "商家回复评价")
    public Result<String> reply(@RequestBody CommentReplyDTO commentReplyDTO) {
        log.info("商家回复评价：{}", commentReplyDTO);
        orderCommentService.replyComment(commentReplyDTO);
        return Result.success();
    }
}
