package de.b3nk4n.gamecloud.orderservice.service;

import de.b3nk4n.gamecloud.orderservice.client.CatalogClient;
import de.b3nk4n.gamecloud.orderservice.model.Order;
import de.b3nk4n.gamecloud.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> submit(String gameId, int quantity) {
        return catalogClient.getGameByGameId(gameId)
                .map(game -> Order.accepted(game, quantity))
                .defaultIfEmpty(Order.rejected(gameId, quantity))
                .flatMap(orderRepository::save);
    }
}
