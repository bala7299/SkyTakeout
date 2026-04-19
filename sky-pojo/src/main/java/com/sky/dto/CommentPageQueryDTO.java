package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 评价分页查询条件（管理端 / 用户端共用）
 * <p>用户端请在 Controller 中设置 userId = BaseContext.getCurrentId()，避免越权查询。</p>
 */
@Data
public class CommentPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    /** 评分：1-5，可选 */
    private Integer score;

    /** 订单 id，可选（多用于管理端） */
    private Long orderId;

    /** 状态：0 未回复，1 已回复，可选（管理端筛选商家是否已回复） */
    private Integer status;

    /** 用户 id，可选（用户端历史评价必设，用于限定本人数据） */
    private Long userId;
}
