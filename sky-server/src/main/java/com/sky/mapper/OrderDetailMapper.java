package com.sky.mapper;

import com.sky.dto.TopSalesItemDTO;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单细明
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据订单id查询订单明细
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> selectByOrderId(Long orderId);

    /**
     * 根据订单ID查询菜品名称（去重）
     * @param orderId 订单ID
     * @return 菜品名称列表
     */
    @Select("SELECT DISTINCT name FROM order_detail WHERE order_id = #{orderId}")
    List<String> getDishNamesByOrderId(Long orderId);

    /**
     * 查询销量前N的菜品/套餐
     * @param limit 限制数量
     * @return 销量排行榜
     */
    List<TopSalesItemDTO> getTopSales(Integer limit);
}
