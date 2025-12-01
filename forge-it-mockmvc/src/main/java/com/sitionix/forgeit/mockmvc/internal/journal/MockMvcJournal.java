package com.sitionix.forgeit.mockmvc.internal.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.mockmvc.internal.domain.MockMvcBuilder;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

@Component
@RequiredArgsConstructor
public class MockMvcJournal {

    private final ObjectMapper objectMapper;

    private final MockMvcLoader mockMvcLoader;

    private final MockMvc mockMvc;

    public <Req, Res> MockMvcBuilder<Req, Res> ping(final Endpoint<Req, Res> endpoint) {
        return new MockMvcBuilder<>(this.mockMvc,
                this.mockMvcLoader,
                this.objectMapper,
                endpoint );
    }
}
