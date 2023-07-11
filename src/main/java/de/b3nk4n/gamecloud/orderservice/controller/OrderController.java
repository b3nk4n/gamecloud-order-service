package de.b3nk4n.gamecloud.orderservice.controller;

import de.b3nk4n.gamecloud.orderservice.model.Order;
import de.b3nk4n.gamecloud.orderservice.model.OrderRequest;
import de.b3nk4n.gamecloud.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Flux<Order> get() {
        return orderService.getAllOrders();
    }

    @PostMapping
    public Mono<Order> post(@Valid @RequestBody OrderRequest order) {
        return orderService.submit(order.gameId(), order.quantity());
    }
}
