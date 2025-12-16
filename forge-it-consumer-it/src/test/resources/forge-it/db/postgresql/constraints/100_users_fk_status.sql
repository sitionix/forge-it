ALTER TABLE users
    ADD CONSTRAINT fk_users_status
        FOREIGN KEY (status_id)
        REFERENCES user_status (status_id);
