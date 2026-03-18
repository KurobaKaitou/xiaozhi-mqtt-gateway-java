package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "gateway.mqtt", name = "enabled", havingValue = "true")
public class MqttInboundMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MqttInboundMessageDispatcher.class);

    private final IMqttControlService mqttControlService;
    private final MqttTopicResolver topicResolver;
    private final IMqttDownlinkPublisher mqttDownlinkPublisher;

    public MqttInboundMessageDispatcher(
            IMqttControlService mqttControlService,
            MqttTopicResolver topicResolver,
            IMqttDownlinkPublisher mqttDownlinkPublisher
    ) {
        this.mqttControlService = mqttControlService;
        this.topicResolver = topicResolver;
        this.mqttDownlinkPublisher = mqttDownlinkPublisher;
    }

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handle(Message<String> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        if (topic == null || message.getPayload() == null) {
            return;
        }

        try {
            String clientId = topicResolver.extractClientId(topic, message.getPayload());
            log.info("mqtt inbound received: topic={}, clientId={}", topic, clientId);
            String response = mqttControlService.handleBrokerPublish(clientId, message.getPayload());
            if (response != null && !response.isBlank()) {
                String responseTopic = topicResolver.buildOutboundTopic(clientId);
                mqttDownlinkPublisher.publishToDevice(clientId, response);
                log.debug("mqtt downlink publish requested: primaryTopic={}, clientId={}", responseTopic, clientId);
            }
        } catch (RuntimeException exception) {
            log.warn("failed to process mqtt inbound message, topic={}", topic, exception);
        }
    }
}
