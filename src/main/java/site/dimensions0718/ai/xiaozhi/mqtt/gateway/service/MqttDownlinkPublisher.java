package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.MqttGatewayProperties;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class MqttDownlinkPublisher implements IMqttDownlinkPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttDownlinkPublisher.class);

    private final ObjectProvider<MessageChannel> mqttOutboundChannelProvider;
    private final MqttTopicResolver mqttTopicResolver;
    private final MqttGatewayProperties mqttGatewayProperties;

    public MqttDownlinkPublisher(
            @Qualifier("mqttOutboundChannel") ObjectProvider<MessageChannel> mqttOutboundChannelProvider,
            MqttTopicResolver mqttTopicResolver,
            MqttGatewayProperties mqttGatewayProperties
    ) {
        this.mqttOutboundChannelProvider = mqttOutboundChannelProvider;
        this.mqttTopicResolver = mqttTopicResolver;
        this.mqttGatewayProperties = mqttGatewayProperties;
    }

    @Override
    public void publishToDevice(String clientId, String payloadJson) {
        MessageChannel channel = mqttOutboundChannelProvider.getIfAvailable();
        if (channel == null) {
            log.warn("mqttOutboundChannel is unavailable, skip downlink publish for clientId={}", clientId);
            return;
        }

        Set<String> topics = new LinkedHashSet<>();
        topics.add(mqttTopicResolver.buildOutboundTopic(clientId));
        if (isHelloPayload(payloadJson)) {
            List<String> compatibilityTopics = mqttGatewayProperties.getCompatibilityOutboundTopics();
            if (compatibilityTopics != null) {
                for (String template : compatibilityTopics) {
                    if (template != null && !template.isBlank()) {
                        topics.add(mqttTopicResolver.resolveTopicTemplate(template, clientId));
                    }
                }
            }
        }

        for (String topic : topics) {
            boolean sent = channel.send(MessageBuilder.withPayload(payloadJson)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build());
            log.info("mqtt downlink publisher sent={}, topic={}, clientId={}", sent, topic, clientId);
        }
    }

    private static boolean isHelloPayload(String payloadJson) {
        try {
            JSONObject json = JSON.parseObject(payloadJson);
            return "hello".equals(json.getString("type"));
        } catch (RuntimeException ignore) {
            return false;
        }
    }
}
