package de.b3nk4n.gamecloud.orderservice.config;

import java.time.Duration;

public record RetryConfig(
        long maxAttempts,
        Duration minBackoff
) {
}
