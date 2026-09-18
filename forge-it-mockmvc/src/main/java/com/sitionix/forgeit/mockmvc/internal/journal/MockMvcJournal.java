package com.sitionix.forgeit.mockmvc.internal.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.domain.MockMvcBuilder;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import com.sitionix.forgeit.domain.endpoint.ServiceContract;
import com.sitionix.forgeit.mockmvc.internal.executor.*;
import org.springframework.core.env.Environment;
import java.net.http.HttpClient;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import java.util.Map;

public class MockMvcJournal {

    private final ObjectMapper objectMapper;

    private final MockMvcLoader mockMvcLoader;

    private final Supplier<MockMvcExecutor> executors;
    private final Function<ServiceContract, MockMvcJournal> serviceBinder;

    private final MockMvcProperties properties;

    public MockMvcJournal(ObjectMapper mapper, MockMvcLoader loader, MockMvc mockMvc, MockMvcProperties properties) {
        this(mapper, loader, properties, () -> new MvcExecutor(mockMvc), null);
    }

    private MockMvcJournal(ObjectMapper mapper, MockMvcLoader loader, MockMvcProperties properties,
                           Supplier<MockMvcExecutor> executors, Function<ServiceContract, MockMvcJournal> binder) {
        this.objectMapper = mapper;
        this.mockMvcLoader = loader;
        this.properties = properties;
        this.executors = executors;
        this.serviceBinder = binder;
    }

    public static MockMvcJournal forHttp(ObjectMapper mapper, MockMvcLoader loader, MockMvcProperties properties,
                                         Environment environment, HttpClient client) {
        return new MockMvcJournal(mapper, loader, properties,
                () -> { throw new IllegalStateException("E2E mockMvc requires a ServiceContract"); },
                contract -> {
                    final var address = ServiceAddress.resolve(contract, environment);
                    final var timeout = HttpExecutor.requireTimeout(properties.getRequestTimeout(), "request-timeout");
                    return new MockMvcJournal(mapper, loader, properties,
                            () -> new HttpExecutor(client, address, timeout), null);
                });
    }

    public MockMvcJournal bind(ServiceContract service) {
        if (this.serviceBinder == null) {
            throw new IllegalStateException("ServiceContract binding requires an unbound E2E journal; IT cannot use external HTTP");
        }
        return this.serviceBinder.apply(service);
    }

    public MockMvcJournal requireUnbound() {
        if (this.serviceBinder != null) {
            throw new IllegalStateException("E2E mockMvc requires a ServiceContract");
        }
        return this;
    }

    public <Req, Res> MockMvcBuilder<Req, Res> ping(final Endpoint<Req, Res> endpoint) {
        final MockMvcBuilder<Req, Res> builder = new MockMvcBuilder<>(this.executors.get(),
                this.mockMvcLoader,
                this.objectMapper,
                endpoint );
        final String defaultToken = this.properties.getDefaultToken();
        if (StringUtils.hasText(defaultToken)) {
            builder.applyDefault(context -> context.token(defaultToken));
        }
        final Map<String, String> defaultHeaders = this.properties.getDefaultHeaders();
        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            builder.applyDefault(context -> defaultHeaders.forEach(context::header));
        }
        return builder;
    }
}
