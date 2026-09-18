package com.sitionix.forgeit.mockmvc.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.ServiceContract;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.journal.MockMvcJournal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class JournalBindingTest {
    @Test void modesMustBeExplicit() {
        var properties = new MockMvcProperties();
        var mvc = new MockMvcJournal(new ObjectMapper(), null,
                (org.springframework.test.web.servlet.MockMvc) null, properties);
        var service = ServiceContract.builder().baseUrl("http://host").build();
        assertSame(mvc, mvc.requireUnbound());
        assertThrows(IllegalStateException.class, () -> mvc.bind(service));
        var http = MockMvcJournal.forHttp(new ObjectMapper(), null, properties, new MockEnvironment(), null);
        assertThrows(IllegalStateException.class, http::requireUnbound);
        assertThrows(IllegalArgumentException.class, () -> http.bind(
                ServiceContract.builder().baseUrlFromProperty("missing").build()));
        assertNotSame(http.bind(service), http.bind(service));
    }
}
