package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.OrderSummaryResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.OrderService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class OrderResolver {

    private final OrderService orderService;

    public OrderResolver(OrderService orderService) {
        this.orderService = orderService;
    }

    @QueryMapping
    public List<OrderSummaryResponse> openOrders(@AuthenticationPrincipal User user) {
        return orderService.getOpenOrders(user);
    }

    @QueryMapping
    public List<OrderSummaryResponse> filledOrders(@AuthenticationPrincipal User user) {
        return orderService.getFilledOrders(user);
    }

    @QueryMapping
    public List<OrderSummaryResponse> orderHistory(@AuthenticationPrincipal User user) {
        return orderService.getOrderHistory(user);
    }
}
