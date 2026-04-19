package com.sky.vo;

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
@Schema(description = "AI智能客服对话响应对象")
public class AiChatVO implements Serializable {

    @Schema(description = "AI客服回复内容")
    private String reply;
}