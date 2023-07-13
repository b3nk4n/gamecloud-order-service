package de.b3nk4n.gamecloud.orderservice.client;

import de.b3nk4n.gamecloud.orderservice.model.Game;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CatalogClient {
    private static final String booksRootApi = "/games/";
    private final WebClient webClient;

    public CatalogClient(WebClient webClient) {
        this.webClient = webClient;
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
                });
    }
}
