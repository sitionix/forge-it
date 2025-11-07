package com.sitionix.forgeit.wiremock.api;

public interface PostgresSupport {
    default String postgress() {
        return"postgres";
    }
}
