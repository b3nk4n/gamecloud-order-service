package de.b3nk4n.gamecloud.orderservice.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.awt.print.Book;
import java.time.LocalDateTime;
import java.util.Date;

@Table("order_table")
public record Order(
        /*
         * The technical ID (surrogate key) for persistence domain.
         */
        @Id
        Long id,
        String gameId,
        String gameTitle,
        Double gamePrice,
        int quantity,
        OrderStatus status,
        @CreatedDate
        LocalDateTime created,
        @LastModifiedDate
        LocalDateTime lastModified,
        @Version
        int version
) {
    public static Order of(String gameId, String gameTitle, Double gamePrice, int quantity, OrderStatus orderStatus) {
        return new Order(null, gameId, gameTitle, gamePrice, quantity, orderStatus, null, null, 0);
    }

    public static Order rejected(String gameId, int quantity) {
        return Order.of(gameId, null, null, quantity, OrderStatus.REJECTED);
    }

    public static Order accepted(Game game, int quantity) {
        return Order.of(game.gameId(), game.title(), game.price(), quantity, OrderStatus.ACCEPTED);
    }

    public static Order dispatched(Order order) {
        return Order.of(order.gameId(), order.gameTitle(), order.gamePrice(), order.quantity, OrderStatus.DISPATCHED);
    }
}
