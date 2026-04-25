package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.AIService;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.utils.BaiduMapUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private BaiduMapUtil baiduMapUtil;
    @Autowired
    private AIService aiService;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private AddressBookService addressBookService;
    private static final Integer TYPE_NEW_ORDER = 1;
    private static final Integer TYPE_ORDER_REMINDER = 2;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        //处理业务异常
        //地址簿是否为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //购物车是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        /**
        //查看是否超出配送范围5km
        String userAddress = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();
        Integer distance = baiduMapUtil.getDistance(baiduMapUtil.getCoordinate(userAddress), baiduMapUtil.getCoordinate(shopAddress));
        if (distance > 5000) {
            throw new OrderBusinessException(MessageConstant.OUT_OF_DELIVERY_DISTANCE);
        }
         */
        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setType(0);
        orderMapper.insert(orders);
        //向订单细明表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空购物车
        ShoppingCart delcart = ShoppingCart.builder().userId(userId).build();
        shoppingCartMapper.deleteByUserId(delcart);
        //封装vo返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().id(orders.getId()).orderTime(orders.getOrderTime()).orderAmount(orders.getAmount()).orderNumber(orders.getNumber()).build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB.getStatus() == Orders.TO_BE_CONFIRMED || ordersDB.getPayStatus() == Orders.PAID) {
            log.info("订单 {} 已经支付过了", outTradeNo);
            return;
        }
        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder().id(ordersDB.getId()).status(Orders.TO_BE_CONFIRMED).payStatus(Orders.PAID).checkoutTime(LocalDateTime.now()).build();
        orderMapper.update(orders);
        //发送来单提醒
        Map map = new HashMap();
        map.put("type",TYPE_NEW_ORDER);
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + orders.getNumber());
        String jsonString = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 分页查询订单
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageForUser(int page, int pageSize, Integer status) {
        Long userId = BaseContext.getCurrentId();
        //构造分页
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status); //根据订单情况查找
        Page<Orders> ordersPage = orderMapper.pageForUser(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        if (ordersPage != null && ordersPage.getTotal() > 0) {
            for (Orders orders : ordersPage) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(orders.getId());
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(ordersPage.getTotal(), list);
    }


    /**
     * 根据id查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        Orders order = orderMapper.getByOrderId(id);
        List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 根据id取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        //确认订单存在
        Orders orders = orderMapper.getByOrderId(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //确认订单状态是否可以取消
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //取消订单 并设置订单状态
        Orders updateOrder = new Orders();
        if (orders.getPayStatus() == Orders.PAID) {
            //订单已支付
            //调用微信支付退款接口
            weChatPayUtil.refund(orders.getNumber(), //商户订单号
                    orders.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            updateOrder.setPayStatus(Orders.REFUND);
        }
        updateOrder.setStatus(Orders.CANCELLED);
        updateOrder.setRejectionReason("用户取消订单");
        updateOrder.setCancelTime(LocalDateTime.now());
        updateOrder.setPayStatus(Orders.REFUND);
        orderMapper.update(updateOrder);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        //查找上一单订单的细明
        List<OrderDetail> orderDetailList = orderDetailMapper.selectByOrderId(id);
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        Long userId = BaseContext.getCurrentId();
        for (OrderDetail orderDetail : orderDetailList) {
            //转化为购物车对象
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id", "orderId");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(shoppingCart);
        }
        //加入购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> ordersPage = orderMapper.PageForadmin(ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(ordersPage);
        return new PageResult(ordersPage.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> ordersPage) {
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = ordersPage.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishs = getOrderDishesStr(orders.getId());
                orderVO.setOrderDishes(orderDishs);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Long id) {
        List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(id);
        StringBuilder sb = new StringBuilder();
        if (!CollectionUtils.isEmpty(orderDetails)) {
            for (OrderDetail orderDetail : orderDetails) {
                sb.append(orderDetail.getName()).append("*").append(orderDetail.getNumber()).append("; ");
            }
        }
        return sb.toString();
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders build = Orders.builder().id(ordersConfirmDTO.getId()).status(Orders.CONFIRMED).build();
        orderMapper.update(build);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    //- 商家拒单其实就是将订单状态修改为“已取消”
    //- 只有订单处于“待接单”状态时可以执行拒单操作
    //- 商家拒单时需要指定拒单原因
    //- 商家拒单时，如果用户已经完成了支付，需要为用户退款
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        if (ordersRejectionDTO == null || ordersRejectionDTO.getId() == null) {
            throw new OrderBusinessException("拒单参数不能为空");
        }
        //获取订单
        Orders orders = orderMapper.getByOrderId(ordersRejectionDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        //查看是否可拒单
        if (orders.getStatus() == Orders.TO_BE_CONFIRMED) {
            //查看用户是否支付
            Orders updateOrder = refund(orders);
            updateOrder.setRejectionReason(ordersRejectionDTO.getRejectionReason());
            orderMapper.update(updateOrder);
        } else {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     */
    //- 取消订单其实就是将订单状态修改为“已取消”
    //- 商家取消订单时需要指定取消原因
    //- 商家取消订单时，如果用户已经完成了支付，需要为用户退款
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        if (ordersCancelDTO == null || ordersCancelDTO.getId() == null) {
            throw new OrderBusinessException("取消订单参数不能为空");
        }
        // 根据id查询订单
        Orders ordersDB = orderMapper.getByOrderId(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        //支付状态
        Orders updateOrder = refund(ordersDB);
        updateOrder.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(updateOrder);
    }

    private Orders refund(Orders orders) throws Exception {
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        if (orders.getPayStatus() == Orders.PAID) {
            weChatPayUtil.refund(orders.getNumber(), //商户订单号
                    orders.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
        }
        //管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        return Orders.builder().status(Orders.CANCELLED).id(orders.getId()).cancelTime(LocalDateTime.now()).payStatus(Orders.REFUND).build();
    }


    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getByOrderId(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    //- 完成订单其实就是将订单状态修改为“已完成”
    //- 只有状态为“派送中”的订单可以执行订单完成操作
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getByOrderId(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);

        // 异步更新用户口味画像
        CompletableFuture.runAsync(() -> {
            try {
                updateUserFlavorProfile(ordersDB);
            } catch (Exception e) {
                log.error("异步更新用户口味画像失败，订单ID: {}, 错误信息: {}", id, e.getMessage());
            }
        });
    }

    /**
     * 更新用户口味画像
     * @param orders 订单信息
     */
    private void updateUserFlavorProfile(Orders orders) {
        try {
            // 获取订单包含的所有菜品/套餐名称
            List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(orders.getId());
            List<String> dishNames = orderDetails.stream()
                    .map(OrderDetail::getName)
                    .collect(java.util.stream.Collectors.toList());

            if (dishNames.isEmpty()) {
                log.warn("订单 {} 中没有菜品信息，跳过口味画像更新", orders.getId());
                return;
            }

            // 获取该用户的当前口味画像
            User user = userMapper.getById(orders.getUserId());
            if (user == null) {
                log.warn("用户 {} 不存在，跳过口味画像更新", orders.getUserId());
                return;
            }

            // 哪怕他是个新用户，我们也得给 Python 传一句人话，不然 AI 不知道怎么接茬
            String currentProfile = (user.getFlavorProfile() != null && !user.getFlavorProfile().trim().isEmpty())
                    ? user.getFlavorProfile()
                    : "暂无历史口味，这是该用户的第一次点单";

            // 调用AI服务更新口味画像
            String newProfile = aiService.updateFlavorProfile(currentProfile, dishNames);

            // 将AI返回的新画像更新到用户表中
            User updateUser = User.builder()
                    .id(user.getId())
                    .flavorProfile(newProfile)
                    .flavorUpdateTime(LocalDateTime.now())
                    .build();
            userMapper.update(updateUser);

            log.info("用户 {} 口味画像更新成功，订单ID: {}", user.getId(), orders.getId());
        } catch (Exception e) {
            log.error("更新用户口味画像失败，订单ID: {}, 错误信息: {}", orders.getId(), e.getMessage());
        }
    }

    /**
     * 客户催单
     *
     * @param id
     */
    public void reminder(Long id) {
        Orders order = orderMapper.getByOrderId(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map = new HashMap();
        map.put("type", TYPE_ORDER_REMINDER);
        map.put("orderId", id);
        map.put("content", "订单号：" + order.getNumber());
        String jsonString = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 创建秒杀订单
     *
     * @param userId 用户ID
     * @param seckillOrderdto 秒杀下单DTO
     * @param seckillPrice 秒杀价格
     * @return 订单ID
     */
    @Transactional
    @Override
    public Long createSeckillOrder(Long userId, SeckillOrderDTO seckillOrderdto, BigDecimal seckillPrice) {
        String itemId = seckillOrderdto.getItemId();
        String itemName;
        String itemImage;
        Long dishId = null;
        Long setmealId = null;

        if (itemId.startsWith("D_")) {
            dishId = Long.parseLong(itemId.substring(2));
            Dish dish = dishMapper.selectById(dishId);
            if (dish == null) {
                throw new OrderBusinessException("秒杀菜品不存在");
            }
            itemName = dish.getName();
            itemImage = dish.getImage();
        } else if (itemId.startsWith("S_")) {
            setmealId = Long.parseLong(itemId.substring(2));
            Setmeal setmeal = setmealMapper.getById(setmealId);
            if (setmeal == null) {
                throw new OrderBusinessException("秒杀套餐不存在");
            }
            itemName = setmeal.getName();
            itemImage = setmeal.getImage();
        } else {
            throw new OrderBusinessException("秒杀活动配置异常");
        }

        // 极速地址装配：优先使用传入的 addressBookId，为空则走 Redis 缓存降级
        AddressBook addressBook;
        Long addressBookId = seckillOrderdto.getAddressBookId();

        if (addressBookId != null) {
            // 场景1：前端传入了指定地址ID，直接查库
            addressBook = addressBookMapper.getById(addressBookId);
            if (addressBook == null) {
                throw new OrderBusinessException("收货地址不存在");
            }
        } else {
            // 场景2：前端未传入地址，优先读取 Redis 默认地址缓存
            addressBook = addressBookService.getDefaultAddress(userId);
            if (addressBook == null) {
                throw new OrderBusinessException("请设置收货地址后再下单");
            }
            addressBookId = addressBook.getId();
        }

        String orderNumber = String.valueOf(System.currentTimeMillis());
        LocalDateTime now = LocalDateTime.now();

        Orders orders = new Orders();
        orders.setNumber(orderNumber);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setAddressBookId(addressBookId);
        orders.setOrderTime(now);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setAmount(seckillPrice);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getDetail());
        orders.setPackAmount(0);
        orders.setTablewareNumber(1);
        orders.setTablewareStatus(1);
        orders.setType(1);
        orders.setRemark(seckillOrderdto.getRemarks());
        orderMapper.insert(orders);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setName(itemName);
        orderDetail.setImage(itemImage);
        orderDetail.setOrderId(orders.getId());
        orderDetail.setDishId(dishId);
        orderDetail.setSetmealId(setmealId);
        orderDetail.setNumber(1);
        orderDetail.setAmount(seckillPrice);
        orderDetailMapper.insertBatch(List.of(orderDetail));

        log.info("秒杀订单创建成功，订单ID：{}，订单号：{}，用户ID：{}，商品：{}，金额：{}", orders.getId(), orderNumber, userId, itemName, seckillPrice);

        return orders.getId();
    }
}
