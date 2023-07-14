package de.b3nk4n.gamecloud.orderservice.controller;

import de.b3nk4n.gamecloud.orderservice.model.*;
import de.b3nk4n.gamecloud.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebFluxTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @Test
    void whenGameNotAvailableThenRejectOrder() {
        final var orderRequest = new OrderRequest("9999", 9);
        final var expectedOrder = Order.rejected(orderRequest.gameId(), orderRequest.quantity());

        given(orderService.submit(orderRequest.gameId(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));

        webTestClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                });
    }

    @Test
    void whenGameAvailableThenAcceptedOrder() {
        final var orderRequest = new OrderRequest("1234", 1);
        final var game = new Game("1234", "FIFA 23", GameGenre.SPORTS, "EA Sports", 39.99);
        final var expectedOrder = Order.accepted(game, orderRequest.quantity());

        given(orderService.submit(orderRequest.gameId(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));

        webTestClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(expectedOrder.status());
                    assertThat(actualOrder.quantity()).isEqualTo(expectedOrder.quantity());
                    assertThat(actualOrder.gameId()).isEqualTo(expectedOrder.gameId());
                    assertThat(actualOrder.gameTitle()).isEqualTo(expectedOrder.gameTitle());
                    assertThat(actualOrder.gamePrice()).isEqualTo(expectedOrder.gamePrice());
                });
    }
}
