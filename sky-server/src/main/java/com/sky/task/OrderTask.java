package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * *")
    public void processTimeOutOrder() {
        log.info("处理超时订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> TimeOutOrders = orderMapper.getTimeOutOrdersLT(Orders.PENDING_PAYMENT, time);
        if (TimeOutOrders != null && TimeOutOrders.size() > 0) {
            for (Orders order : TimeOutOrders) {
                //取消超时订单
                Orders cancelOrder = Orders.builder().id(order.getId()).status(Orders.CANCELLED).
                        cancelTime(LocalDateTime.now()).cancelReason("订单超时，自动取消").build();
                orderMapper.update(cancelOrder);
            }
        }
    }
   // 0 0 1 * * ?
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOutOrder() {
        log.info("定时处理处于派送中的订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> DeliveryOrders = orderMapper.getTimeOutOrdersLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (DeliveryOrders != null && DeliveryOrders.size() > 0) {
            for (Orders order : DeliveryOrders) {
                //取消仍处于派送中的订单
                Orders deliveryOrder = Orders.builder().id(order.getId()).status(Orders.COMPLETED).build();
                orderMapper.update(deliveryOrder);
            }
        }
    }
}