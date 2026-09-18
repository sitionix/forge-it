package com.sitionix.forgeit.ros.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;

public final class RosDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains("forge-it-ros-default.yml")) return;
        try {
            for (var source : new YamlPropertySourceLoader().load("forge-it-ros-default.yml",
                    new ClassPathResource("forge-it-ros-default.yml"))) {
                environment.getPropertySources().addLast(source);
            }
        } catch (IOException ex) { throw new IllegalStateException("Cannot load ForgeIT ROS defaults"); }
    }
    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
