package com.sky.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "用户端历史评价返回的数据格式")
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("订单id")
    private Long orderId;

    @ApiModelProperty("评分：1-5分")
    private Integer score;

    @ApiModelProperty("原始评论内容")
    private String content;

    @ApiModelProperty("商家回复内容")
    private String replyContent;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}

