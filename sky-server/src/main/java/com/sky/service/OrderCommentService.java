package com.sky.service;

import com.sky.dto.CommentPageQueryDTO;
import com.sky.dto.CommentReplyDTO;
import com.sky.dto.CommentSubmitDTO;
import com.sky.result.PageResult;

public interface OrderCommentService {

    /**
     * 用户提交评价
     *
     * @param commentSubmitDTO 提交评价数据
     */
    void submitComment(CommentSubmitDTO commentSubmitDTO);

    /**
     * 管理端评价分页查询
     *
     * @param commentPageQueryDTO 分页与筛选条件
     * @return 分页结果
     */
    PageResult pageQuery(CommentPageQueryDTO commentPageQueryDTO);

    /**
     * 用户端：分页查询当前登录用户的历史评价（入参需已设置 userId 等条件）
     *
     * @param commentPageQueryDTO 分页与筛选条件（须包含当前用户 userId）
     * @return 分页结果，记录为 CommentVO
     */
    PageResult pageQuery4User(CommentPageQueryDTO commentPageQueryDTO);

    /**
     * 管理端：商家回复评价
     *
     * @param commentReplyDTO 评价id与回复内容
     */
    void replyComment(CommentReplyDTO commentReplyDTO);
}

