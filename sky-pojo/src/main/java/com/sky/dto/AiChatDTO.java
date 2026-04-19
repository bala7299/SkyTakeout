package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "AI智能客服对话请求参数")
public class AiChatDTO implements Serializable {

    @ApiModelProperty(value = "用户发送的消息内容", required = true)
    private String message;
}