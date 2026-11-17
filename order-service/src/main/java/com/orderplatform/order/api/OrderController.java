package com.orderplatform.order.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderplatform.order.domain.Order;
import com.orderplatform.order.domain.OrderItem;
import com.orderplatform.order.domain.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Returns 202 Accepted: the order stays PENDING until inventory confirms or rejects it asynchronously. */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request,
            Authentication auth) {
        List<OrderItem> items = request.lines().stream().map(l -> new OrderItem(l.sku(), l.quantity())).toList();
        Order order = orderService.placeOrder(auth.getName(), items);
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id, Authentication auth) {
        Order order = orderService.getOrder(id);
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !order.getCustomerId().equals(auth.getName())) {
            throw new AccessDeniedException("Not your order");
        }
        return OrderResponse.from(order);
    }

    @GetMapping
    public List<OrderResponse> myOrders(Authentication auth) {
        return orderService.getOrdersFor(auth.getName()).stream().map(OrderResponse::from).toList();
    }
}
