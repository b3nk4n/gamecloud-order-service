package de.b3nk4n.gamecloud.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    WebClient webClient(ClientConfig config, WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(config.catalogServiceUri().toString())
                .build();
    }
}
