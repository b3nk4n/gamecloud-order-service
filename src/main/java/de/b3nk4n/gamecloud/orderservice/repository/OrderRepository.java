package de.b3nk4n.gamecloud.orderservice.repository;

import de.b3nk4n.gamecloud.orderservice.model.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Flux<Order> findAllByCreator(String username);
}
