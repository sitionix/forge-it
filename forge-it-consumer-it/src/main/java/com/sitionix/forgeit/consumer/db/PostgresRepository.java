package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.consumer.db.jpa.PostgresJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostgresRepository {

    private final PostgresJpaRepository jpaRepository;

    public UserEntity createPostgres(final UserEntity request) {
        return this.jpaRepository.save(request);
    }
}
