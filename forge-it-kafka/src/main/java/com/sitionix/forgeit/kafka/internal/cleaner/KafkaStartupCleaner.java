package com.sitionix.forgeit.kafka.internal.cleaner;

import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DependsOn("kafkaContainerManager")
@ConditionalOnProperty(prefix = "forge-it.modules.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public final class KafkaStartupCleaner implements InitializingBean {

    private final KafkaTopicCleaner topicCleaner;
    private final KafkaProperties properties;

    @Override
    public void afterPropertiesSet() {
        if (this.properties.getMode() == KafkaProperties.Mode.EXTERNAL) {
            log.debug("Skipping Kafka startup cleanup for external mode.");
            return;
        }
        this.topicCleaner.reset();
    }
}
