package com.sky.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户提交评价时传递的数据模型")
public class CommentSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单id", required = true)
    private Long orderId;

    @Schema(description = "评分：1-5分", required = true)
    private Integer score;

    @Schema(description = "原始评论内容")
    private String content;
}

