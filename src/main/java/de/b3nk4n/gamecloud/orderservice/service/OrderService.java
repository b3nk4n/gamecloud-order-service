package de.b3nk4n.gamecloud.orderservice.service;

import de.b3nk4n.gamecloud.orderservice.client.CatalogClient;
import de.b3nk4n.gamecloud.orderservice.message.OrderAcceptedMessage;
import de.b3nk4n.gamecloud.orderservice.message.OrderDispatchedMessage;
import de.b3nk4n.gamecloud.orderservice.model.Order;
import de.b3nk4n.gamecloud.orderservice.model.OrderStatus;
import de.b3nk4n.gamecloud.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final StreamBridge streamBridge;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient,
                        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") StreamBridge streamBridge) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.streamBridge = streamBridge;
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional // following the Saga pattern, that persisting to the DB and sending an event about it should happen atomically
    public Mono<Order> submit(String gameId, int quantity) {
        return catalogClient.getGameByGameId(gameId)
                .map(game -> Order.accepted(game, quantity))
                .defaultIfEmpty(Order.rejected(gameId, quantity))
                .flatMap(orderRepository::save)
                .doOnNext(this::sendOrderAcceptedMessage);
    }

    public void sendOrderAcceptedMessage(Order order) {
        if (OrderStatus.ACCEPTED != order.status()) {
            return;
        }

        final var orderAcceptedMessage = new OrderAcceptedMessage(order.id());

        final var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);

        log.info("Sent accepted message with ID={} and result: {}", order.id(), result);
    }

    public Flux<Order> consumeOrderDispatchedMessage(Flux<OrderDispatchedMessage> orderDispatchedMessageFlux) {
        return orderDispatchedMessageFlux
                .flatMap(orderDispatchedMessage -> orderRepository.findById(orderDispatchedMessage.orderId()))
                .map(Order::dispatched)
                .flatMap(orderRepository::save);
    }
}
