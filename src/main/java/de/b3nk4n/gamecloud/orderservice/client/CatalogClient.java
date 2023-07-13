package de.b3nk4n.gamecloud.orderservice.client;

import de.b3nk4n.gamecloud.orderservice.model.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class CatalogClient {
    private static final String booksRootApi = "/games/";
    private final WebClient webClient;

    @Value("${client.timeout}")
    private Duration clientTimeout;

    @Value("${client.retry.max-attempts}")
    private long clientRetryMaxAttempts;
    @Value("${client.retry.min-backoff}")
    private Duration clientRetryMinBackoff;

    @Autowired
    public CatalogClient(WebClient webClient) {
        this.webClient = webClient;
    }

    CatalogClient(WebClient webClient, Duration timeout, long maxAttempts, Duration minBackoff) {
        this.webClient = webClient;
        this.clientTimeout = timeout;
        this.clientRetryMaxAttempts = maxAttempts;
        this.clientRetryMinBackoff = minBackoff;
    }

    public Mono<Game> getGameByGameId(String gameId) {
        return webClient
                .get()
                .uri(booksRootApi + gameId)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.empty();
                    }
                    return clientResponse.bodyToMono(Game.class);
                })
//                 //alternative:
//                 .retrieve()
//                 .bodyToMono(Game.class);
//                 .onErrorResume(WebClientResponseException.NotFound.class,
//                      exception -> Mono.empty())
                .timeout(clientTimeout, Mono.empty())
                .retryWhen(Retry.backoff(clientRetryMaxAttempts, clientRetryMinBackoff))
                .onErrorResume(Exception.class, ex -> Mono.empty());
    }
}
