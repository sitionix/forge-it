package com.sitionix.forgeit.wiremock.internal;

import com.sitionix.forgeit.testing.MockExtension;
import com.sitionix.forgeit.wiremock.internal.configs.WireMockContainerManager;
import com.sitionix.forgeit.wiremock.internal.configs.WireMockProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WireMockContainerManagerTests extends MockExtension {

    private static final List<String> EXPORTED_PROPERTIES = List.of(
            "forgeit.wiremock.base-url",
            "forgeit.wiremock.port",
            "forgeit.wiremock.host"
    );

    @Test
    void shouldUseExternalWireMockConfiguration() {
        final WireMockProperties properties = new WireMockProperties();
        properties.setEnabled(true);
        properties.setMode(WireMockProperties.Mode.EXTERNAL);
        properties.setHost("localhost");
        properties.setPort(9090);

        final MockEnvironment environment = new MockEnvironment();
        final WireMockContainerManager manager = new WireMockContainerManager(environment, properties);

        manager.afterPropertiesSet();

        final WireMockState runningState = captureRunningState(manager, environment);

        manager.destroy();

        final EnvironmentState destroyedState = captureEnvironment(environment);

        final WireMockLifecycleState actualState = new WireMockLifecycleState(runningState, destroyedState);

        final WireMockLifecycleState expectedState = new WireMockLifecycleState(
                new WireMockState(URI.create("http://localhost:9090"), Map.of(
                        "forgeit.wiremock.base-url", "http://localhost:9090",
                        "forgeit.wiremock.port", "9090",
                        "forgeit.wiremock.host", "localhost"
                )),
                new EnvironmentState(Map.of())
        );

        assertThat(actualState).isEqualTo(expectedState);
    }

    @Test
    void shouldFailWhenFeatureDisabled() {
        final WireMockProperties properties = new WireMockProperties();
        properties.setEnabled(false);

        final WireMockContainerManager manager = new WireMockContainerManager(new MockEnvironment(), properties);

        assertThatThrownBy(manager::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    private static WireMockState captureRunningState(WireMockContainerManager manager, MockEnvironment environment) {
        return new WireMockState(manager.getBaseUrl(), captureWireMockProperties(environment));
    }

    private static EnvironmentState captureEnvironment(MockEnvironment environment) {
        return new EnvironmentState(captureWireMockProperties(environment));
    }

    private static Map<String, String> captureWireMockProperties(MockEnvironment environment) {
        final Map<String, String> captured = new LinkedHashMap<>();
        for (String key : EXPORTED_PROPERTIES) {
            if (environment.containsProperty(key)) {
                captured.put(key, environment.getProperty(key));
            }
        }
        return captured;
    }

    private record WireMockLifecycleState(WireMockState runningState, EnvironmentState environmentAfterDestroy) {
    }

    private record WireMockState(URI baseUrl, Map<String, String> properties) {
    }

    private record EnvironmentState(Map<String, String> properties) {
    }
}
