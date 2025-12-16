package com.sitionix.forgeit.consumer.db.jpa;

import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostgresJpaRepository extends JpaRepository<UserEntity, Long> {
}
