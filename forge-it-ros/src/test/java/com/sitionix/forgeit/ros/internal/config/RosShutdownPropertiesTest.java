package com.sitionix.forgeit.ros.internal.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import java.time.Duration;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class RosShutdownPropertiesTest {
    @Test void bindsUserConfiguredDeadline() {
        var binder = new Binder(new MapConfigurationPropertySource(
                Map.of("forge-it.modules.ros.shutdown-timeout", "750ms")));
        var properties = binder.bind("forge-it.modules.ros", Bindable.of(RosProperties.class)).get();
        assertThat(properties.getShutdownTimeout()).isEqualTo(Duration.ofMillis(750));
    }
    @Test void rejectsMissingOrNonpositiveDeadline() {
        for (Duration invalid : new Duration[] {null, Duration.ZERO, Duration.ofNanos(-1)}) {
            assertThatThrownBy(() -> new RosProperties().setShutdownTimeout(invalid))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("shutdown-timeout must be positive");
        }
        for (String invalid : new String[] {"0s", "-1s"}) {
            var binder = new Binder(new MapConfigurationPropertySource(
                    Map.of("forge-it.modules.ros.shutdown-timeout", invalid)));
            assertThatThrownBy(() -> binder.bind("forge-it.modules.ros", Bindable.of(RosProperties.class)))
                    .hasMessageContaining("forge-it.modules.ros");
        }
    }
}
