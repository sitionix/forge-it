package com.sitionix.forgeit.mockmvc.internal.domain;

import com.sitionix.forgeit.domain.endpoint.ServiceContract;
import com.sitionix.forgeit.mockmvc.internal.executor.ServiceAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class ServiceContractTest {
    @Test
    void keepsDefinitionAndResolvesSeparatelyForEachContext() {
        var contract = ServiceContract.builder().baseUrlFromProperty("service.url").build();
        assertEquals("${service.url}", contract.getBaseUrl());
        assertEquals("https://one.test/api", ServiceAddress.resolve(contract,
                new MockEnvironment().withProperty("service.url", "https://one.test/api")).toString());
        assertEquals("http://two.test", ServiceAddress.resolve(contract,
                new MockEnvironment().withProperty("service.url", "http://two.test")).toString());
        assertEquals("${service.url}", contract.getBaseUrl());
    }

    @Test
    void validatesDefinitionsAndResolvedAddresses() {
        assertThrows(IllegalArgumentException.class, () -> ServiceContract.builder().baseUrlFromProperty(" "));
        assertThrows(IllegalStateException.class, () -> ServiceContract.builder().build());
        var property = ServiceContract.builder().baseUrlFromProperty("service.url").build();
        assertThrows(IllegalArgumentException.class, () -> ServiceAddress.resolve(property, new MockEnvironment()));
        for (String value : new String[]{"", " ", "${missing}", "ftp://host", "http:///path", "http://host?q=x",
                "http://user:secret@host", "http://host/#x", "http://host:99999", "http://host/a b"}) {
            assertThrows(IllegalArgumentException.class, () -> ServiceAddress.resolve(property,
                    new MockEnvironment().withProperty("service.url", value)), value);
        }
        assertEquals("https://host/api", ServiceAddress.resolve(
                ServiceContract.builder().baseUrl("https://host/api").build(), new MockEnvironment()).toString());
    }
}
