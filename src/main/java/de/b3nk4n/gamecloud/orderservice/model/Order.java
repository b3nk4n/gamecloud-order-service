package de.b3nk4n.gamecloud.orderservice.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

@Table("orders") // configures the mapping between the "Order" object and the "orders" table
public record Order(
        /*
         * The technical ID (surrogate key) for persistence domain.
         */
        @Id
        Long id,
        String gameId,
        String gameTitle,
        double gamePrice,
        int quantity,
        OrderStatus orderStatus,
        @CreatedDate
        Date created,
        @LastModifiedDate
        Date lastModified,
        @Version
        int version
) {
    public static Order of(String gameId, String gameTitle, double gamePrice, int quantity, OrderStatus orderStatus) {
        return new Order(null, gameId, gameTitle,gamePrice, quantity, orderStatus, null, null, 0);
    }
}
