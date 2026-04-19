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
@Schema(description = "商家回复评价时传递的数据模型")
public class CommentReplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评价主键id", required = true)
    private Long id;

    @Schema(description = "商家回复内容", required = true)
    private String replyContent;
}
