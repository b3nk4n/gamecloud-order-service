package de.b3nk4n.gamecloud.orderservice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.b3nk4n.gamecloud.orderservice.client.CatalogClient;
import de.b3nk4n.gamecloud.orderservice.message.OrderAcceptedMessage;
import de.b3nk4n.gamecloud.orderservice.model.Game;
import de.b3nk4n.gamecloud.orderservice.model.GameGenre;
import de.b3nk4n.gamecloud.orderservice.model.Order;
import de.b3nk4n.gamecloud.orderservice.model.OrderRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("integrationtest")
@Tag("IntegrationTest")
@Testcontainers
class OrderServiceApplicationTest {
    /**
     * Has role employee & customer.
     */
    private static KeycloakToken bennyToken;

    /**
     * Has only role customer.
     */
    private static KeycloakToken vanessaToken;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.3"));

    @Container
    static KeycloakContainer keycloakContainer = new KeycloakContainer("keycloak/keycloak:22.0.1")
            .withRealmImportFile("keycloak-realm-test-config.json");

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private ObjectMapper objectMapper;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private OutputDestination outputDestination;

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

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakContainer.getAuthServerUrl() + "/realms/GameCloud");
    }

    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s",
                postgres.getHost(),
                postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgres.getDatabaseName());
    }

    @BeforeAll
    static void initAccessTokens() {
        final var webClient = WebClient.builder()
                .baseUrl(keycloakContainer.getAuthServerUrl() + "/realms/GameCloud/protocol/openid-connect/token")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        bennyToken = authenticateWith(webClient, "b3nk4n", "password");
        vanessaToken = authenticateWith(webClient, "vankwk", "password");
    }

    private static KeycloakToken authenticateWith(WebClient webClient, String username, String password) {
        return webClient
                .post()
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "gamecloud-test")
                        .with("username", username)
                        .with("password", password))
                .retrieve()
                .bodyToMono(KeycloakToken.class)
                .block();
    }

    @Test
    void whenPostExistingGameAuthenticatedThenSendAcceptedMessageAndReturnOnGet() throws IOException {
        final Game game = new Game("1234", "FIFA 23", GameGenre.SPORTS, "EA Sports", 39.99);
        final OrderRequest orderRequest = new OrderRequest(game.gameId(), 1);

        given(catalogClient.getGameByGameId(game.gameId())).willReturn(Mono.just(game));

        final Order expectedOrder = webTestClient.post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(bennyToken.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();

        assertThat(expectedOrder).isNotNull();
        assertThat(receiveOrderAcceptedMessage()).isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient.get()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(bennyToken.accessToken()))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Order.class).value(orders ->
                        assertThat(orders.stream()
                                .filter(order -> order.gameId().equals(game.gameId()))
                                .findAny())
                                .isNotEmpty());
    }

    @Test
    void whenGetOrdersUnauthenticatedThen401() {
        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenPostOrderUnauthenticatedThen401() {
        final var game = new Game("1234", "FIFA 23", GameGenre.SPORTS, "EA Sports", 39.99);
        final var orderRequest = new OrderRequest(game.gameId(), 1);

        webTestClient.post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenGetOwnOrdersThenReturn() throws IOException {
        final var gameId = "1234";
        final var game = new Game(gameId, "FIFA 23", GameGenre.SPORTS, "EA Sports", 39.99);
        final var orderRequest = new OrderRequest(gameId, 1);

        given(catalogClient.getGameByGameId(gameId)).willReturn(Mono.just(game));

        final var expectedOrder = webTestClient.post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(vanessaToken.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();
        
        assertThat(expectedOrder).isNotNull();
        assertThat(receiveOrderAcceptedMessage()).isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient.get().uri("/orders")
                .headers(headers -> headers.setBearerAuth(vanessaToken.accessToken()))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(orders -> {
                    final var orderIds = orders.stream()
                            .map(Order::id)
                            .toList();
                    assertThat(orderIds).contains(expectedOrder.id());
                });
    }

    @Test
    void whenGetOrdersThenOnlyReturnOwnOrders() throws IOException {
        final var gameId = "1234";
        final var game = new Game(gameId, "FIFA 23", GameGenre.SPORTS, "EA Sports", 39.99);
        final var orderRequest = new OrderRequest(game.gameId(), 1);

        given(catalogClient.getGameByGameId(gameId)).willReturn(Mono.just(game));

        final var vanessaOrder = webTestClient.post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(vanessaToken.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();

        assertThat(vanessaOrder).isNotNull();
        assertThat(receiveOrderAcceptedMessage()).isEqualTo(new OrderAcceptedMessage(vanessaOrder.id()));

        final var otherGameId = "2345";
        final var otherGame = new Game(otherGameId, "NHL 23", GameGenre.SPORTS, "EA Sports", 29.99);
        final var otherOrderRequest = new OrderRequest(otherGame.gameId(), 2);

        given(catalogClient.getGameByGameId(otherGameId)).willReturn(Mono.just(otherGame));

        final var bennyOrder = webTestClient.post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(bennyToken.accessToken()))
                .bodyValue(otherOrderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();

        assertThat(bennyOrder).isNotNull();
        assertThat(receiveOrderAcceptedMessage()).isEqualTo(new OrderAcceptedMessage(bennyOrder.id()));

        webTestClient.get().uri("/orders")
                .headers(headers -> headers.setBearerAuth(vanessaToken.accessToken()))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(orders -> {
                    // TODO ensure that each test method is executed independently, which is not the case when the testcontainer
                    //      is declared as STATIC. Which is however required when using @DynamicPropertySource.
                    // assertThat(orders).hasSize(1);

                    final var orderIds = orders.stream()
                            .map(Order::id)
                            .toList();
                    assertThat(orderIds).contains(vanessaOrder.id());
                    assertThat(orderIds).doesNotContain(bennyOrder.id());
                });
    }

    private OrderAcceptedMessage receiveOrderAcceptedMessage() throws IOException {
        return objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class);
    }

    private record KeycloakToken(String accessToken) {
        @JsonCreator
        private KeycloakToken(@JsonProperty("access_token") String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
