package com.sitionix.forgeit.mockmvc.internal.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.domain.MockMvcBuilder;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MockMvcJournal {

    private final ObjectMapper objectMapper;

    private final MockMvcLoader mockMvcLoader;

    private final MockMvc mockMvc;

    private final MockMvcProperties properties;

    public <Req, Res> MockMvcBuilder<Req, Res> ping(final Endpoint<Req, Res> endpoint) {
        final MockMvcBuilder<Req, Res> builder = new MockMvcBuilder<>(this.mockMvc,
                this.mockMvcLoader,
                this.objectMapper,
                endpoint );
        final String defaultToken = this.properties.getDefaultToken();
        if (StringUtils.hasText(defaultToken)) {
            builder.applyDefault(context -> context.token(defaultToken));
        }
        return builder;
    }
}
