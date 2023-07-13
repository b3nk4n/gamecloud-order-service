package de.b3nk4n.gamecloud.orderservice.model;

public record Game(
        String gameId,
        String title,
        GameGenre genre,
        String publisher,
        double price
) {
}
