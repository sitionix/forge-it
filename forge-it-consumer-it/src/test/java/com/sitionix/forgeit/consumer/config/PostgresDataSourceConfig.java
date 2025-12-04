package com.sitionix.forgeit.consumer.config;


import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class PostgresDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/forge-it");
        dataSource.setUsername("forge-it");
        dataSource.setPassword("forge-it-pwd");
        dataSource.setDriverClassName("org.postgresql.Driver");

        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);

        return dataSource;
    }
}

