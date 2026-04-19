package com.sky.config;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class AiFunctionConfig {
    // record类 相当于dto，用于封装函数的请求参数和响应结果
    public record OrderQueryRequest(String orderNumber) {
    }

    public record OrderQueryResponse(Integer status, String statusDesc, String deliveryStatus) {
    }

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    @Description("当用户想要查询外卖订单状态、进度时调用此函数。必须从用户的话语中提取出订单号(orderNumber)。")
    // Function接口，用于封装函数的请求参数和响应结果 相当于把函数的逻辑封装起来，方便调用
    // Function<T, R> 表示函数的输入参数为T，输出结果为R
    // 形参为OrderMapper，这里为参数注入，用于查询订单信息，因为打了@Bean注解，Spring会从容器中找到OrderMapper的实现类，并自动注入
    // 为什么
    public Function<OrderQueryRequest, OrderQueryResponse> orderInfoFunction(OrderMapper orderMapper) {
        return request -> {
            try {
                Orders order = orderMapper.getByNumber(request.orderNumber());
                if (order == null) {
                    return new OrderQueryResponse(null, "未查到该订单，请核对订单号", null);
                }

                Integer status = order.getStatus();
                String statusDesc = parseOrderStatus(status);
                String deliveryStatus = parseDeliveryStatus(status);

                return new OrderQueryResponse(status, statusDesc, deliveryStatus);
            } catch (Exception e) {
                return new OrderQueryResponse(null, "查询订单时发生异常，请稍后再试", null);
            }
        };
    }

    private String parseOrderStatus(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case 1:
                return "待付款";
            case 2:
                return "待接单";
            case 3:
                return "已接单";
            case 4:
                return "派送中";
            case 5:
                return "已完成";
            case 6:
                return "已取消";
            default:
                return "未知状态";
        }
    }

    private String parseDeliveryStatus(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (status == 4) {
            return "骑手正在火速配送中，请耐心等待哦~";
        } else if (status == 5) {
            return "订单已送达";
        } else if (status == 6) {
            return "订单已取消";
        } else {
            return "订单尚未配送";
        }
    }
}