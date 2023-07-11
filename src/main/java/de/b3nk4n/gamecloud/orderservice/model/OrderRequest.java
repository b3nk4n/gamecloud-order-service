package de.b3nk4n.gamecloud.orderservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderRequest(
        @NotBlank(message = "The game ID must be defined.")
        String gameId,
        @Min(value = 1, message = "You must order at least 1 game.")
        @Max(value = 9, message = "You cannot purchase more than 9 games in a single order.")
        int quantity
) { }
