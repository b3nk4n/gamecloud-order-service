package de.b3nk4n.gamecloud.orderservice.repository;

import de.b3nk4n.gamecloud.orderservice.config.OrderDataConfig;
import de.b3nk4n.gamecloud.orderservice.model.Game;
import de.b3nk4n.gamecloud.orderservice.model.GameGenre;
import de.b3nk4n.gamecloud.orderservice.model.Order;
import de.b3nk4n.gamecloud.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(OrderDataConfig.class) // needed to enable auditing
@Testcontainers
class OrderRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.3"));

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Overwrites R2DBC and Flyway configuration to point to the test Postgres instance
     */
    @DynamicPropertySource
    static void postgresConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderRepositoryTest::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
    }

    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s",
                postgres.getHost(),
                postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgres.getDatabaseName());
    }

    @Test
    void createRejectedOrder() {
        final var rejectedOrder = Order.rejected("9999", 9);

        StepVerifier
                .create(orderRepository.save(rejectedOrder))
                .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
                .verifyComplete();
    }

    @Test
    void createAcceptedOrder() {
        final var game = new Game("1234", "FIFA 23", GameGenre.SPORTS, "EA Sports", 39.99);
        final var acceptedOrder = Order.accepted(game, 1);

        StepVerifier
                .create(orderRepository.save(acceptedOrder))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
                    assertThat(order.gameId()).isEqualTo(game.gameId());
                    assertThat(order.gameTitle()).isEqualTo(game.title());
                    assertThat(order.gamePrice()).isEqualTo(game.price());
                    assertThat(order.quantity()).isEqualTo(acceptedOrder.quantity());
                })
                .verifyComplete();
    }
}
