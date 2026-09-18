package com.sitionix.forgeit.mockmvc.internal.executor;

import com.sitionix.forgeit.domain.endpoint.ServiceContract;
import org.springframework.core.env.Environment;
import java.net.URI;

public final class ServiceAddress {
    private ServiceAddress() { }

    public static URI resolve(final ServiceContract contract, final Environment environment) {
        if (contract == null) {
            throw new IllegalArgumentException("ServiceContract must be provided");
        }
        // Do not expose property values, credentials or URL query strings in configuration errors.
        try {
            final String value = environment.resolveRequiredPlaceholders(contract.getBaseUrl());
            final URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("ServiceContract requires a resolved, nonblank HTTP(S) base URL "
                    + "with a host and without credentials, query or fragment");
        }
    }
}
