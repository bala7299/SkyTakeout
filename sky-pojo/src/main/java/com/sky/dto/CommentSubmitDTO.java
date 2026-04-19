package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "用户提交评价时传递的数据模型")
public class CommentSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单id", required = true)
    private Long orderId;

    @ApiModelProperty(value = "评分：1-5分", required = true)
    private Integer score;

    @ApiModelProperty("原始评论内容")
    private String content;
}

