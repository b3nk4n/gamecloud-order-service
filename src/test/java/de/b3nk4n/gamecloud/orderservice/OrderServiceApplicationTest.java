package de.b3nk4n.gamecloud.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.b3nk4n.gamecloud.orderservice.client.CatalogClient;
import de.b3nk4n.gamecloud.orderservice.config.OrderDataConfig;
import de.b3nk4n.gamecloud.orderservice.message.OrderAcceptedMessage;
import de.b3nk4n.gamecloud.orderservice.model.*;
import de.b3nk4n.gamecloud.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class OrderServiceApplicationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.3"));

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private ObjectMapper objectMapper;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private OutputDestination outputDestination;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private InputDestination inputDestination;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CatalogClient catalogClient;

    /**
     * Overwrites R2DBC and Flyway configuration to point to the test Postgres instance
     */
    @DynamicPropertySource
    static void postgresConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderServiceApplicationTest::r2dbcUrl);
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
    void whenPostExistingGameThenSendAcceptedMessageAndReturnOnGet() throws IOException {
        final Game game = new Game("1234", "FIFA 23", GameGenre.SPORTS, "EA Sports", 39.99);
        final OrderRequest orderRequest = new OrderRequest(game.gameId(), 1);

        given(catalogClient.getGameByGameId(game.gameId())).willReturn(Mono.just(game));

        final Order expectedOrder = webTestClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Order.class).value(orders ->
                        assertThat(orders.stream().filter(order -> order.gameId().equals(game.gameId())).findAny())
                                .isNotEmpty());
    }
}
