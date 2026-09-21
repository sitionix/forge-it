package com.sitionix.forgeit.bundleconsumer;

import com.sitionix.forgeit.core.test.E2E;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.ServiceContract;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@E2E
class BundleHttpE2ETest {
    @Autowired private BundleSupport forgeIt;

    @Test
    void generatedSupportMakesRealHttpRequestFromSingleBundleDependency() throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/smoke", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            var service = ServiceContract.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
            forgeIt.mockMvc(service)
                    .ping(Endpoint.createContract("/smoke", HttpMethod.GET, Void.class, Void.class))
                    .expectStatus(HttpStatus.NO_CONTENT).assertAndCreate();
            assertThat(calls.get()).isEqualTo(1);
            assertThat(forgeIt.getClass().getSimpleName()).isEqualTo("BundleSupportImpl");
        } finally {
            server.stop(0);
        }
    }
}
