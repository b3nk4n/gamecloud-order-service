CREATE TABLE order_table
(
    id              BIGSERIAL PRIMARY KEY NOT NULL,
    game_id         varchar(255)          NOT NULL,
    game_title      varchar(255),
    game_price      float8,
    quantity        int                   NOT NULL,
    status          varchar(255)          NOT NULL,
    created         timestamp             NOT NULL,
    last_modified   timestamp             NOT NULL,
    version         integer               NOT NULL
);
