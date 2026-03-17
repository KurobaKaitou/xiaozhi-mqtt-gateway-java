package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class MqttDownlinkPublisher implements IMqttDownlinkPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttDownlinkPublisher.class);

    private final ObjectProvider<MessageChannel> mqttOutboundChannelProvider;
    private final MqttTopicResolver mqttTopicResolver;

    public MqttDownlinkPublisher(
            @Qualifier("mqttOutboundChannel") ObjectProvider<MessageChannel> mqttOutboundChannelProvider,
            MqttTopicResolver mqttTopicResolver
    ) {
        this.mqttOutboundChannelProvider = mqttOutboundChannelProvider;
        this.mqttTopicResolver = mqttTopicResolver;
    }

    @Override
    public void publishToDevice(String clientId, String payloadJson) {
        MessageChannel channel = mqttOutboundChannelProvider.getIfAvailable();
        if (channel == null) {
            log.warn("mqttOutboundChannel is unavailable, skip downlink publish for clientId={}", clientId);
            return;
        }

        String topic = mqttTopicResolver.buildOutboundTopic(clientId);
        boolean sent = channel.send(MessageBuilder.withPayload(payloadJson)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build());
        log.info("mqtt downlink publisher sent={}, topic={}, clientId={}", sent, topic, clientId);
    }
}
