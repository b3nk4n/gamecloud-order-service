package de.b3nk4n.gamecloud.orderservice.service;

import de.b3nk4n.gamecloud.orderservice.model.Order;
import de.b3nk4n.gamecloud.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> submit(String gameId, int quantity) {
        return Mono.just(Order.rejected(gameId, quantity))
                .flatMap(orderRepository::save);
    }
}
