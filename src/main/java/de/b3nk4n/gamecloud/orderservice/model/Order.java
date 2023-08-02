package de.b3nk4n.gamecloud.orderservice.model;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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
        @CreatedBy
        String creator,
        @LastModifiedBy
        String lastModifier,
        @Version
        int version
) {
    public static Order of(String gameId, String gameTitle, Double gamePrice, int quantity, OrderStatus orderStatus) {
        return new Order(null, gameId, gameTitle, gamePrice, quantity, orderStatus, null, null, null, null, 0);
    }

    public static Order rejected(String gameId, int quantity) {
        return Order.of(gameId, null, null, quantity, OrderStatus.REJECTED);
    }

    public static Order accepted(Game game, int quantity) {
        return Order.of(game.gameId(), game.title(), game.price(), quantity, OrderStatus.ACCEPTED);
    }

    public static Order dispatched(Order order) {
        return new Order(
                order.id(), order.gameId(), order.gameTitle(), order.gamePrice(), order.quantity(), OrderStatus.DISPATCHED,
                order.created(), order.lastModified(), order.creator(), order.lastModifier(), order.version());
    }
}
