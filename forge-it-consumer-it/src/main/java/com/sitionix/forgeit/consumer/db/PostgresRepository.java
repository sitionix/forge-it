package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.sitionix.forgeit.consumer.db.jpa.PostgresJpaRepository;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge-it.modules.postgresql", name = "enabled", havingValue = "true")
public class PostgresRepository {

    private final ObjectProvider<PostgresJpaRepository> jpaRepository;

    public UserEntity createPostgres(final UserEntity request) {
        final PostgresJpaRepository repository = this.jpaRepository.getIfAvailable();
        if (repository == null) {
            throw new IllegalStateException("Postgresql support is not enabled for this test context");
        }
        return repository.save(request);
    }
}
