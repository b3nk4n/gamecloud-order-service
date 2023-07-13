package de.b3nk4n.gamecloud.orderservice.client;

import de.b3nk4n.gamecloud.orderservice.model.Game;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class CatalogClientTest {
    private MockWebServer mockWebServer;
    private CatalogClient catalogClient;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build();
        catalogClient = new CatalogClient(webClient, Duration.ofSeconds(1), 1, Duration.ofMillis(100));
    }

    @AfterEach
    void cleanUp() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void whenGameExistsThenReturnGame() {
        final var gameId = "1234";

        final var mockResponse = new MockResponse()
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "gameId": %s,
                          "title": "FIFA 23",
                          "genre": "SPORTS",
                          "publisher": "EA Sports",
                          "price": 39.99
                        }
                        """.formatted(gameId));

        mockWebServer.enqueue(mockResponse);

        Mono<Game> game = catalogClient.getGameByGameId(gameId);

        StepVerifier.create(game)
                .expectNextMatches(b -> b.gameId().equals(gameId))
                .verifyComplete();
    }
}
