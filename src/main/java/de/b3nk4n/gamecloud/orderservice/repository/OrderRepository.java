package de.b3nk4n.gamecloud.orderservice.repository;

import de.b3nk4n.gamecloud.orderservice.model.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
