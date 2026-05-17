package com.sky.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端评价分页查询返回的数据格式")
public class AdminCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单id")
    private Long orderId;

    @Schema(description = "评分：1-5分")
    private Integer score;

    @Schema(description = "原始评论内容")
    private String content;

    @Schema(description = "AI润色后的建议回复")
    private String aiOptimized;

    @Schema(description = "商家回复内容")
    private String replyContent;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "状态：0未回复，1已回复")
    private Integer status;

    @Schema(description = "评价人用户名")
    private String userName;

    @Schema(description = "业务订单号")
    private String orderNumber;
}
