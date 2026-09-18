package com.sitionix.forgeit.domain.endpoint;

public final class ServiceContractBuilder {
    private String baseUrl;

    ServiceContractBuilder() {
    }

    public ServiceContractBuilder baseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public ServiceContractBuilder baseUrlFromProperty(final String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must be provided");
        }
        this.baseUrl = "${" + propertyKey + "}";
        return this;
    }

    public ServiceContract build() {
        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            throw new IllegalStateException("Service base URL must be provided");
        }
        return new ServiceContract(this.baseUrl);
    }
}
