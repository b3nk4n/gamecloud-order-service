package de.b3nk4n.gamecloud.orderservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "client")
public record ClientConfig(
        @NotNull
        URI catalogServiceUri,
        Duration timeout
) {
}
