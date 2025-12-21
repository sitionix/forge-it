ALTER TABLE products
    ADD CONSTRAINT fk_products_users
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE;
