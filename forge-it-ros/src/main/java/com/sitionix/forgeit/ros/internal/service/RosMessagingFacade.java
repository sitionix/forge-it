package com.sitionix.forgeit.ros.internal.service;

import com.sitionix.forgeit.ros.api.RosConsumeBuilder;
import com.sitionix.forgeit.ros.api.RosMessaging;
import com.sitionix.forgeit.ros.api.RosPublishBuilder;
import com.sitionix.forgeit.ros.api.RosTopicContract;
import com.sitionix.forgeit.ros.internal.config.RosProperties;
import com.sitionix.forgeit.ros.internal.domain.DefaultRosConsumeBuilder;
import com.sitionix.forgeit.ros.internal.domain.DefaultRosPublishBuilder;
import com.sitionix.forgeit.ros.internal.loader.RosLoader;
import com.sitionix.forgeit.ros.internal.port.RosConsumerPort;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import org.springframework.core.env.Environment;

public final class RosMessagingFacade implements RosMessaging {

    private final RosLoader rosLoader;
    private final Environment environment;
    private final RosProperties properties;
    private final RosPublisherPort publisherPort;
    private final RosConsumerPort consumerPort;

    public RosMessagingFacade(final RosLoader rosLoader, final Environment environment,
                              final RosProperties properties, final RosPublisherPort publisherPort,
                              final RosConsumerPort consumerPort) {
        this.rosLoader = rosLoader;
        this.environment = environment;
        this.properties = properties;
        this.publisherPort = publisherPort;
        this.consumerPort = consumerPort;
    }

    @Override
    public RosPublishBuilder publish(final RosTopicContract contract) {
        return new DefaultRosPublishBuilder(contract, this.rosLoader, this.environment,
                this.properties, this.publisherPort, this.consumerPort);
    }

    @Override
    public RosConsumeBuilder consume(final RosTopicContract contract) {
        return new DefaultRosConsumeBuilder(contract, this.rosLoader, this.environment,
                this.properties, this.consumerPort);
    }
}
