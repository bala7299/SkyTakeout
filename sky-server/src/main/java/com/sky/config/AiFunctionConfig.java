package com.sky.config;

import com.sky.context.BaseContext;
import com.sky.entity.Dish;
import com.sky.entity.OrderComment;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderCommentMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.OrderService;
import com.sky.service.RecommendService;
import com.sky.vo.RecommendItemVO;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Configuration
public class AiFunctionConfig {

    public record OrderQueryRequest(String orderNumber) {}

    public record OrderQueryResponse(Integer status, String statusDesc, String deliveryStatus) {}

    public record CancelOrderRequest(String orderNumber) {}

    public record CancelOrderResponse(Boolean success, String message) {}

    public record RecommendByTasteRequest(String dummy) {}

    public record RecommendByTasteResponse(String message, List<RecommendItemVO> recommendItems) {}

    public record SearchDishRequest(String dishName, String categoryName, String maxPrice) {}

    public record SearchDishResponse(String message, String dishList) {}

    public record GetDishReviewsRequest(String dishName) {}

    public record GetDishReviewsResponse(String message, String reviewList) {}

    public record ReOrderRequest(String dummy) {}

    public record ReOrderResponse(Boolean success, String message) {}

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    @Description("【强制指令】当用户表达出以下任何意图时，必须调用此函数：1. 查询订单状态或进度（如'我的订单到哪了'、'外卖到哪了'、'什么时候送到'、'订单怎么样了'）；2. 询问订单是否已接单/已配送/已完成；3. 想知道外卖还要多久。必须从用户话语中提取订单号(orderNumber)。如果用户未提供订单号，请先引导用户提供，不要自行编造。")
    public Function<OrderQueryRequest, OrderQueryResponse> orderInfoFunction(OrderMapper orderMapper) {
        return request -> {
            try {
                BaseContext.setCurrentIntent("orderInfoFunction");
                Orders order = orderMapper.getByNumber(request.orderNumber());
                OrderQueryResponse result;
                if (order == null) {
                    result = new OrderQueryResponse(null, "未查到该订单，请核对订单号", null);
                } else {
                    Integer status = order.getStatus();
                    String statusDesc = parseOrderStatus(status);
                    String deliveryStatus = parseDeliveryStatus(status);
                    result = new OrderQueryResponse(status, statusDesc, deliveryStatus);
                }
                BaseContext.setFunctionData(result);
                return result;
            } catch (Exception e) {
                OrderQueryResponse result = new OrderQueryResponse(null, "查询订单时发生异常，请稍后再试", null);
                BaseContext.setFunctionData(result);
                return result;
            }
        };
    }

    @Bean
    @Description("【强制指令】当用户表达出以下任何意图时，必须调用此函数：1. 想要取消订单（如'取消订单'、'不要了'、'不想点了'）；2. 申请退款或退单（如'退款'、'退单'、'想退'）；3. 对已下的订单表示后悔或不想继续。必须从用户话语中提取订单号(orderNumber)。如果用户未提供订单号，请先引导用户提供，不要自行编造。")
    public Function<CancelOrderRequest, CancelOrderResponse> cancelOrderFunction(OrderService orderService, OrderMapper orderMapper) {
        return request -> {
            try {
                BaseContext.setCurrentIntent("cancelOrderFunction");
                Orders order = orderMapper.getByNumber(request.orderNumber());
                CancelOrderResponse result;
                if (order == null) {
                    result = new CancelOrderResponse(false, "抱歉，未找到订单号为 " + request.orderNumber() + " 的订单，请核对后再试哦~");
                } else if (order.getStatus() == Orders.COMPLETED) {
                    result = new CancelOrderResponse(false, "该订单已完成，无法取消哦。如有问题请联系客服处理~");
                } else if (order.getStatus() == Orders.CANCELLED) {
                    result = new CancelOrderResponse(false, "该订单已经取消啦，无需重复操作哦~");
                } else {
                    orderService.userCancelById(order.getId());
                    result = new CancelOrderResponse(true, "订单 " + request.orderNumber() + " 已成功取消，退款将原路返回，请耐心等待~");
                }
                BaseContext.setFunctionData(result);
                return result;
            } catch (Exception e) {
                CancelOrderResponse result = new CancelOrderResponse(false, "取消订单时遇到问题：" + e.getMessage() + "，请稍后再试或联系客服处理~");
                BaseContext.setFunctionData(result);
                return result;
            }
        };
    }

    @Bean
    @Description("【强制指令】当用户表达出以下任何意图时，必须调用此函数：1. 请求推荐菜品（如'推荐'、'推荐一下'、'有什么推荐'）；2. 询问有什么好吃的（如'有什么好吃的'、'店里有什么'、'招牌菜是什么'）；3. 表达不知道吃什么（如'不知道吃什么'、'纠结吃什么'、'帮我选'、'随便'）；4. 提到饿了或想点餐（如'饿了'、'想吃东西'、'想点外卖'、'点餐'）。严禁直接通过通用对话回复菜品名称，必须通过此函数查询数据库中用户的口味偏好和真实菜品信息，以确保返回结构化的functionData供前端渲染卡片。如果无法获取用户偏好，则查询系统热销菜品作为兜底。无需提取参数。")
    public Function<RecommendByTasteRequest, RecommendByTasteResponse> recommendByTasteFunction(RecommendService recommendService, UserMapper userMapper) {
        return request -> {
            try {
                BaseContext.setCurrentIntent("recommendByTasteFunction");
                Long userId = BaseContext.getCurrentId();
                User user = userMapper.getById(userId);

                if (user == null) {
                    RecommendByTasteResponse result = new RecommendByTasteResponse("抱歉，未找到您的用户信息，无法为您推荐~", List.of());
                    BaseContext.setFunctionData(result);
                    return result;
                }

                String userProfile = user.getFlavorProfile();
                if (userProfile == null || userProfile.trim().isEmpty()) {
                    var result = recommendService.getAIRecommendation();
                    List<RecommendItemVO> items = result.getItems() != null ? result.getItems() : List.of();
                    BaseContext.setFunctionData(items);
                    return new RecommendByTasteResponse("您还没有点过外卖呢~ 不过别担心，我为您推荐了店里最受欢迎的菜品，快来看看吧！", items);
                }

                var result = recommendService.getAIRecommendation();
                String recommendText = result.getRecommendation() != null ? result.getRecommendation() : "为您推荐以下美味菜品~";
                List<RecommendItemVO> items = result.getItems() != null ? result.getItems() : List.of();

                // 将结构化数据存入BaseContext，供AIServiceImpl取出传给前端
                BaseContext.setFunctionData(items);

                return new RecommendByTasteResponse(recommendText, items);
            } catch (Exception e) {
                RecommendByTasteResponse result = new RecommendByTasteResponse("抱歉，推荐菜品时出了点小问题：" + e.getMessage() + "，请稍后再试~", List.of());
                BaseContext.setFunctionData(result);
                return result;
            }
        };
    }

    @Bean
    @Description("【强制指令】当用户表达出以下任何意图时，必须调用此函数：1. 按名称搜索菜品（如'有没有水煮鱼'、'搜一下宫保鸡丁'）；2. 按分类浏览菜品（如'有什么荤菜'、'看看主食'、'饮料有哪些'）；3. 按价格区间筛选（如'50元以下的'、'便宜点的'、'最贵多少'）；4. 组合条件搜索（如'50元以下的辣味菜'、'荤菜里有什么便宜的'）。需提取dishName(可选，菜品名称关键词)、categoryName(可选，分类名称)、maxPrice(可选，最高价格，纯数字)。所有参数都是可选的，至少提供一个。严禁直接通过通用对话编造菜品信息。")
    public Function<SearchDishRequest, SearchDishResponse> searchDishFunction(DishMapper dishMapper) {
        return request -> {
            try {
                BaseContext.setCurrentIntent("searchDishFunction");
                String dishName = request.dishName();
                String categoryName = request.categoryName();
                String maxPriceStr = request.maxPrice();

                List<Dish> dishes = new ArrayList<>();

                if (dishName != null && !dishName.trim().isEmpty()
                        && categoryName != null && !categoryName.trim().isEmpty()
                        && maxPriceStr != null && !maxPriceStr.trim().isEmpty()) {
                    BigDecimal maxPrice = new BigDecimal(maxPriceStr);
                    dishes = dishMapper.searchByConditions(dishName, categoryName, maxPrice);
                } else if (dishName != null && !dishName.trim().isEmpty()) {
                    dishes = dishMapper.searchByName(dishName);
                } else if (categoryName != null && !categoryName.trim().isEmpty()) {
                    dishes = dishMapper.searchByCategory(categoryName);
                } else if (maxPriceStr != null && !maxPriceStr.trim().isEmpty()) {
                    BigDecimal maxPrice = new BigDecimal(maxPriceStr);
                    dishes = dishMapper.searchByMaxPrice(maxPrice);
                } else {
                    SearchDishResponse result = new SearchDishResponse("请告诉我您想搜索什么样的菜品哦~ 比如菜名、分类或者价格范围都可以~", "");
                    BaseContext.setFunctionData(result);
                    return result;
                }

                if (dishes == null || dishes.isEmpty()) {
                    SearchDishResponse result = new SearchDishResponse("抱歉，没有找到符合条件的菜品，换个条件试试吧~", "");
                    BaseContext.setFunctionData(result);
                    return result;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(dishes.size(), 10); i++) {
                    Dish d = dishes.get(i);
                    sb.append(String.format("%d. %s - ￥%s", i + 1, d.getName(), d.getPrice()));
                    if (d.getFlavorTag() != null && !d.getFlavorTag().isEmpty()) {
                        sb.append(" [").append(d.getFlavorTag()).append("]");
                    }
                    sb.append("\n");
                }

                SearchDishResponse result = new SearchDishResponse("找到以下菜品，快来看看吧~", sb.toString());
                BaseContext.setFunctionData(result);
                return result;
            } catch (Exception e) {
                SearchDishResponse result = new SearchDishResponse("搜索菜品时出了点小问题：" + e.getMessage() + "，请稍后再试~", "");
                BaseContext.setFunctionData(result);
                return result;
            }
        };
    }

    @Bean
    @Description("【强制指令】当用户表达出以下任何意图时，必须调用此函数：1. 询问某道菜好不好吃（如'水煮鱼好吃吗'、'这个菜怎么样'）；2. 查看菜品评价或口碑（如'评价怎么样'、'口碑如何'、'评分多少'）；3. 想了解其他人对菜品的看法（如'别人怎么说'、'有人吃过吗'）；4. 在点餐前犹豫想参考意见。必须从用户话语中提取菜品名称(dishName)。如果用户描述模糊，请先确认具体菜名再调用。")
    public Function<GetDishReviewsRequest, GetDishReviewsResponse> getDishReviewsFunction(OrderCommentMapper orderCommentMapper) {
        return request -> {
            try {
                BaseContext.setCurrentIntent("getDishReviewsFunction");
                String dishName = request.dishName();
                GetDishReviewsResponse result;
                if (dishName == null || dishName.trim().isEmpty()) {
                    result = new GetDishReviewsResponse("请告诉我您想查询哪道菜的评价哦~", "");
                    BaseContext.setFunctionData(result);
                    return result;
                }

                List<OrderComment> reviews = orderCommentMapper.getTopReviewsByDishName(dishName);

                if (reviews == null || reviews.isEmpty()) {
                    result = new GetDishReviewsResponse("暂时还没有关于 " + dishName + " 的评价呢，不过相信它一定很好吃~", "");
                    BaseContext.setFunctionData(result);
                    return result;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < reviews.size(); i++) {
                    OrderComment r = reviews.get(i);
                    sb.append(String.format("%d. 评分：%d分 | %s", i + 1, r.getScore(), r.getContent()));
                    if (r.getReplyContent() != null && !r.getReplyContent().isEmpty()) {
                        sb.append("\n   商家回复：").append(r.getReplyContent());
                    }
                    sb.append("\n");
                }

                result = new GetDishReviewsResponse("以下是 " + dishName + " 的精选好评，快来看看吧~", sb.toString());
                BaseContext.setFunctionData(result);
                return result;
            } catch (Exception e) {
                GetDishReviewsResponse result = new GetDishReviewsResponse("查询评价时出了点小问题：" + e.getMessage() + "，请稍后再试~", "");
                BaseContext.setFunctionData(result);
                return result;
            }
        };
    }

    @Bean
    @Description("【强制指令】当用户表达出以下任何意图时，必须调用此函数：1. 想要再来一单（如'再来一单'、'再来一份'、'按之前的再买一份'）；2. 想重复之前的订单（如'和上次一样'、'还是上次那些'、'照着上次点'）；3. 想快速复购（如'还吃上次的'、'老样子'）。无需参数，自动获取用户最近一次已完成的订单并将菜品加入购物车。")
    public Function<ReOrderRequest, ReOrderResponse> reOrderFunction(OrderMapper orderMapper, OrderDetailMapper orderDetailMapper, ShoppingCartMapper shoppingCartMapper) {
        return request -> {
            try {
                BaseContext.setCurrentIntent("reOrderFunction");
                Long userId = BaseContext.getCurrentId();

                Orders lastOrder = orderMapper.getLastCompletedOrderByUserId(userId);
                ReOrderResponse result;
                if (lastOrder == null) {
                    result = new ReOrderResponse(false, "抱歉，没有找到您之前的订单，无法再来一单哦~ 请先下单后再使用此功能~");
                    BaseContext.setFunctionData(result);
                    return result;
                }

                List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(lastOrder.getId());
                if (orderDetails == null || orderDetails.isEmpty()) {
                    result = new ReOrderResponse(false, "抱歉，该订单中没有找到菜品信息，无法再来一单~");
                    BaseContext.setFunctionData(result);
                    return result;
                }

                List<ShoppingCart> shoppingCartList = new ArrayList<>();
                for (OrderDetail detail : orderDetails) {
                    ShoppingCart cart = ShoppingCart.builder()
                            .name(detail.getName())
                            .image(detail.getImage())
                            .userId(userId)
                            .dishId(detail.getDishId())
                            .setmealId(detail.getSetmealId())
                            .dishFlavor(detail.getDishFlavor())
                            .number(detail.getNumber())
                            .amount(detail.getAmount())
                            .createTime(LocalDateTime.now())
                            .build();
                    shoppingCartList.add(cart);
                }

                shoppingCartMapper.insertBatch(shoppingCartList);

                result = new ReOrderResponse(true, "已将您上次订单中的 " + shoppingCartList.size() + " 件商品加入购物车，快去结算吧~");
                BaseContext.setFunctionData(result);
                return result;
            } catch (Exception e) {
                ReOrderResponse result = new ReOrderResponse(false, "再来一单时出了点小问题：" + e.getMessage() + "，请稍后再试~");
                BaseContext.setFunctionData(result);
                return result;
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
