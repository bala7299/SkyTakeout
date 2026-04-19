package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单评价表，对应表 sky_take_out.order_comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderComment implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键
    private Long id;

    // 订单id
    private Long orderId;

    // 用户id
    private Long userId;

    // 评分：1-5分
    private Integer score;

    // 原始评论内容
    private String content;

    // AI润色后的建议回复
    private String aiOptimized;

    // 创建时间
    private LocalDateTime createTime;

    // 商家回复内容
    private String replyContent;

    // 状态：0未回复，1已回复
    private Integer status;
}

