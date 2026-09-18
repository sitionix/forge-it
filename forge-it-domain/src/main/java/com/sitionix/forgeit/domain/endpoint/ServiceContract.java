package com.sitionix.forgeit.domain.endpoint;

/** A reusable address definition, resolved independently in each test context. */
public final class ServiceContract {
    private final String baseUrl;

    ServiceContract(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static ServiceContractBuilder builder() {
        return new ServiceContractBuilder();
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }
}
