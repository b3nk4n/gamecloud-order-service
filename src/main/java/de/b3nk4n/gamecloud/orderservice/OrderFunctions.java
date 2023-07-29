package de.b3nk4n.gamecloud.orderservice;

import de.b3nk4n.gamecloud.orderservice.message.OrderDispatchedMessage;
import de.b3nk4n.gamecloud.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class OrderFunctions {
    private final static Logger log = LoggerFactory.getLogger(OrderFunctions.class);

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> orderDispatched(OrderService orderService) {
        return orderDispatchedMessageFlux ->
             orderService.consumeOrderDispatchedMessage(orderDispatchedMessageFlux)
                     .doOnNext(order -> log.info("Order with ID={} is dispatched.", order.id()))
                     .subscribe();
    }
}
