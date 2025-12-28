package com.sitionix.forgeit.consumer.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "consumer.kafka")
@ConditionalOnProperty(prefix = "forge-it.modules.kafka", name = "enabled", havingValue = "true")
public class KafkaTopicConfig {

    private boolean enabled;
    private String bootstrapServers = "localhost:9092";
    private String inputTopic = "forge-it-input-topic";
    private String outputTopic = "forge-it-output-topic";
    private String payloadInputTopic = "forge-it-payload-input-topic";
    private String payloadOutputTopic = "forge-it-payload-output-topic";
    private String groupId = "forge-it-consumer";
    private String clientId = "forge-it-client";

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getBootstrapServers() {
        return this.bootstrapServers;
    }

    public void setBootstrapServers(final String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getInputTopic() {
        return this.inputTopic;
    }

    public void setInputTopic(final String inputTopic) {
        this.inputTopic = inputTopic;
    }

    public String getOutputTopic() {
        return this.outputTopic;
    }

    public void setOutputTopic(final String outputTopic) {
        this.outputTopic = outputTopic;
    }

    public String getPayloadInputTopic() {
        return this.payloadInputTopic;
    }

    public void setPayloadInputTopic(final String payloadInputTopic) {
        this.payloadInputTopic = payloadInputTopic;
    }

    public String getPayloadOutputTopic() {
        return this.payloadOutputTopic;
    }

    public void setPayloadOutputTopic(final String payloadOutputTopic) {
        this.payloadOutputTopic = payloadOutputTopic;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }
}
