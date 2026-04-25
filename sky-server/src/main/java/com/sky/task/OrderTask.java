package com.sky.task;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
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

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedissonClient redissonClient;

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

                // 差异化处理：秒杀订单需要回滚库存
                if (order.getType() != null && order.getType() == 1) {
                    rollbackSeckillStock(order);
                }
            }
        }
    }

    /**
     * 回滚秒杀订单库存
     *
     * @param order 超时的秒杀订单
     */
    private void rollbackSeckillStock(Orders order) {
        try {
            List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(order.getId());
            if (orderDetails == null || orderDetails.isEmpty()) {
                log.warn("秒杀订单 {} 无明细数据，无法回滚库存", order.getId());
                return;
            }

            for (OrderDetail detail : orderDetails) {
                String itemId = null;
                if (detail.getDishId() != null) {
                    itemId = "D_" + detail.getDishId();
                } else if (detail.getSetmealId() != null) {
                    itemId = "S_" + detail.getSetmealId();
                }

                if (itemId != null) {
                    String stockKey = "seckill:stock:" + itemId;
                    RAtomicLong atomicStock = redissonClient.getAtomicLong(stockKey);
                    if (atomicStock.isExists()) {
                        long newStock = atomicStock.incrementAndGet();
                        log.info("秒杀订单 {} 超时取消，回滚库存成功，itemId：{}，Redis 库存：{}", order.getId(), itemId, newStock);
                    } else {
                        log.warn("秒杀订单 {} 超时取消，但 Redis 中不存在库存 Key：{}，跳过回滚", order.getId(), stockKey);
                    }
                }
            }
        } catch (Exception e) {
            log.error("秒杀订单 {} 库存回滚失败，错误信息：{}", order.getId(), e.getMessage(), e);
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