package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     *
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据用户id分页查询订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageForUser(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单
     *
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getByOrderId(Long id);

    /**
     * 根据订单id查询订单详情
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> PageForadmin(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单状态查询订单数量
     *
     * @param orderStatus
     * @return
     */
    @Select("select count(*) from orders where status = #{orderStatus}")
    Integer countStatus(Integer orderStatus);

    /**
     * 查询超时订单
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getTimeOutOrdersLT(Integer status, LocalDateTime time);

    /**
     * 查询当天营业总额
     * @param map
     * @return
     */
    BigDecimal getSumByDate(Map map);

    /**
     * 根据条件查询订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 查询销量排行榜
     * @param map
     * @return
     */
    List<GoodsSalesDTO> goodsSalesTop10(Map map);
}
