package com.sky.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "AI智能客服对话请求参数")
public class AiChatDTO implements Serializable {

    @Schema(description = "用户发送的消息内容", required = true)
    private String message;
}