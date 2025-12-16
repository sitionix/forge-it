CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL,
    status_id  BIGINT NOT NULL
);
