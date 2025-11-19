package com.sitionix.forgeit.wiremock;

import com.sitionix.forgeit.domain.JsonLoader;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLoaderIntegrationTest {

    @Test
    void shouldExposeApplicationLayerJsonLoaderThroughDomainInterface() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.scan("com.sitionix.forgeit.application");
            context.refresh();

            JsonLoader jsonLoader = context.getBean(JsonLoader.class);

            assertThat(jsonLoader.load()).isEqualTo("application-layer-json");
        }
    }
}
