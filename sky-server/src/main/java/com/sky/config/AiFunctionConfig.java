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

    public record RecommendByTasteResponse(String message, String recommendItems) {}

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
    @Description("当用户想要查询外卖订单状态、进度时调用此函数。必须从用户的话语中提取出订单号(orderNumber)。")
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

    @Bean
    @Description("当用户想要取消订单或申请退单时调用此函数。必须从用户的话语中提取出订单号(orderNumber)。")
    public Function<CancelOrderRequest, CancelOrderResponse> cancelOrderFunction(OrderService orderService, OrderMapper orderMapper) {
        return request -> {
            try {
                Orders order = orderMapper.getByNumber(request.orderNumber());
                if (order == null) {
                    return new CancelOrderResponse(false, "抱歉，未找到订单号为 " + request.orderNumber() + " 的订单，请核对后再试哦~");
                }

                if (order.getStatus() == Orders.COMPLETED) {
                    return new CancelOrderResponse(false, "该订单已完成，无法取消哦。如有问题请联系客服处理~");
                }

                if (order.getStatus() == Orders.CANCELLED) {
                    return new CancelOrderResponse(false, "该订单已经取消啦，无需重复操作哦~");
                }

                orderService.userCancelById(order.getId());
                return new CancelOrderResponse(true, "订单 " + request.orderNumber() + " 已成功取消，退款将原路返回，请耐心等待~");
            } catch (Exception e) {
                return new CancelOrderResponse(false, "取消订单时遇到问题：" + e.getMessage() + "，请稍后再试或联系客服处理~");
            }
        };
    }

    @Bean
    @Description("当用户询问'吃什么'、'推荐菜品'、'有什么好吃的'时调用此函数。无需提取参数，直接根据用户口味画像推荐菜品。")
    public Function<RecommendByTasteRequest, RecommendByTasteResponse> recommendByTasteFunction(RecommendService recommendService, UserMapper userMapper) {
        return request -> {
            try {
                Long userId = BaseContext.getCurrentId();
                User user = userMapper.getById(userId);

                if (user == null) {
                    return new RecommendByTasteResponse("抱歉，未找到您的用户信息，无法为您推荐~", "");
                }

                String userProfile = user.getFlavorProfile();
                if (userProfile == null || userProfile.trim().isEmpty()) {
                    return new RecommendByTasteResponse("您还没有点过外卖呢~ 不过别担心，我为您推荐了店里最受欢迎的菜品，快来看看吧！", "热销推荐");
                }

                var result = recommendService.getAIRecommendation();
                String recommendText = result.getRecommendation() != null ? result.getRecommendation() : "为您推荐以下美味菜品~";
                String items = result.getItems() != null ? result.getItems().toString() : "";

                return new RecommendByTasteResponse(recommendText, items);
            } catch (Exception e) {
                return new RecommendByTasteResponse("抱歉，推荐菜品时出了点小问题：" + e.getMessage() + "，请稍后再试~", "");
            }
        };
    }

    @Bean
    @Description("当用户想要搜索菜品时调用此函数。用户可能会按名称、分类或价格区间搜索（例如：'50元以下的辣味菜'）。需提取 dishName(可选，菜品名称关键词), categoryName(可选，分类名称), maxPrice(可选，最高价格)。所有参数都是可选的，至少提供一个。")
    public Function<SearchDishRequest, SearchDishResponse> searchDishFunction(DishMapper dishMapper) {
        return request -> {
            try {
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
                    return new SearchDishResponse("请告诉我您想搜索什么样的菜品哦~ 比如菜名、分类或者价格范围都可以~", "");
                }

                if (dishes == null || dishes.isEmpty()) {
                    return new SearchDishResponse("抱歉，没有找到符合条件的菜品，换个条件试试吧~", "");
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

                return new SearchDishResponse("找到以下菜品，快来看看吧~", sb.toString());
            } catch (Exception e) {
                return new SearchDishResponse("搜索菜品时出了点小问题：" + e.getMessage() + "，请稍后再试~", "");
            }
        };
    }

    @Bean
    @Description("当用户询问某道菜好不好吃、评价如何、口碑怎么样时调用此函数。必须从用户的话语中提取出菜品名称(dishName)。")
    public Function<GetDishReviewsRequest, GetDishReviewsResponse> getDishReviewsFunction(OrderCommentMapper orderCommentMapper) {
        return request -> {
            try {
                String dishName = request.dishName();
                if (dishName == null || dishName.trim().isEmpty()) {
                    return new GetDishReviewsResponse("请告诉我您想查询哪道菜的评价哦~", "");
                }

                List<OrderComment> reviews = orderCommentMapper.getTopReviewsByDishName(dishName);

                if (reviews == null || reviews.isEmpty()) {
                    return new GetDishReviewsResponse("暂时还没有关于 " + dishName + " 的评价呢，不过相信它一定很好吃~", "");
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

                return new GetDishReviewsResponse("以下是 " + dishName + " 的精选好评，快来看看吧~", sb.toString());
            } catch (Exception e) {
                return new GetDishReviewsResponse("查询评价时出了点小问题：" + e.getMessage() + "，请稍后再试~", "");
            }
        };
    }

    @Bean
    @Description("当用户说'再来一单'、'按之前的再买一份'、'再来一份'时调用此函数。无需参数，自动获取用户最近一次已完成的订单，并将订单中的菜品加入购物车。")
    public Function<ReOrderRequest, ReOrderResponse> reOrderFunction(OrderMapper orderMapper, OrderDetailMapper orderDetailMapper, ShoppingCartMapper shoppingCartMapper) {
        return request -> {
            try {
                BaseContext.setCurrentId(5L);
                Long userId = BaseContext.getCurrentId();

                Orders lastOrder = orderMapper.getLastCompletedOrderByUserId(userId);
                if (lastOrder == null) {
                    return new ReOrderResponse(false, "抱歉，没有找到您之前的订单，无法再来一单哦~ 请先下单后再使用此功能~");
                }

                List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(lastOrder.getId());
                if (orderDetails == null || orderDetails.isEmpty()) {
                    return new ReOrderResponse(false, "抱歉，该订单中没有找到菜品信息，无法再来一单~");
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

                return new ReOrderResponse(true, "已将您上次订单中的 " + shoppingCartList.size() + " 件商品加入购物车，快去结算吧~");
            } catch (Exception e) {
                return new ReOrderResponse(false, "再来一单时出了点小问题：" + e.getMessage() + "，请稍后再试~");
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
