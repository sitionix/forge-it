package com.sitionix.forgeit.consumer.db.contract;

import com.sitionix.forgeit.application.sql.cleaner.DefaultDbCleanupPlan;
import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;
import com.sitionix.forgeit.domain.contract.clean.DbCleanupPlan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CleanUpStrategy {

    @Bean
    public DbCleanupPlan dbCleanupPlan() {
        return DefaultDbCleanupPlan.builder()
                .add(CleanupPhase.BEFORE_EACH,
                        DbContracts.USER_ENTITY_DB_CONTRACT)
                .build();
    }
 }
