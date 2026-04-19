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
@ApiModel(description = "商家回复评价时传递的数据模型")
public class CommentReplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "评价主键id", required = true)
    private Long id;

    @ApiModelProperty(value = "商家回复内容", required = true)
    private String replyContent;
}
