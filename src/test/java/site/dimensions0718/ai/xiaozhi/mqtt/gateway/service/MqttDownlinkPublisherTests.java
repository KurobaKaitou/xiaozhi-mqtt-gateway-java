package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.MqttGatewayProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttDownlinkPublisherTests {

    @Test
    void shouldFanoutHelloToCompatibilityTopics() {
        MessageChannel channel = Mockito.mock(MessageChannel.class);
        Mockito.when(channel.send(Mockito.any(Message.class))).thenReturn(true);

        @SuppressWarnings("unchecked")
        ObjectProvider<MessageChannel> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(provider.getIfAvailable()).thenReturn(channel);

        MqttGatewayProperties properties = new MqttGatewayProperties();
        properties.setOutboundTopicTemplate("devices/p2p/{macRaw}");
        properties.setCompatibilityOutboundTopics(List.of("null", "device/{clientId}/down"));

        MqttTopicResolver resolver = new MqttTopicResolver(properties);
        MqttDownlinkPublisher publisher = new MqttDownlinkPublisher(provider, resolver, properties);

        String clientId = "nulllab-AI-VOX3@@@10_20_ba_40_0a_40@@@10_20_ba_40_0a_40";
        publisher.publishToDevice(clientId, "{\"type\":\"hello\"}");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(channel, Mockito.times(3)).send(captor.capture());

        List<Object> topics = captor.getAllValues().stream()
                .map(message -> message.getHeaders().get(MqttHeaders.TOPIC))
                .toList();

        assertTrue(topics.contains("devices/p2p/10_20_ba_40_0a_40"));
        assertTrue(topics.contains("null"));
        assertTrue(topics.contains("device/" + clientId + "/down"));
    }

    @Test
    void shouldUsePrimaryTopicOnlyForNonHelloPayload() {
        MessageChannel channel = Mockito.mock(MessageChannel.class);
        Mockito.when(channel.send(Mockito.any(Message.class))).thenReturn(true);

        @SuppressWarnings("unchecked")
        ObjectProvider<MessageChannel> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(provider.getIfAvailable()).thenReturn(channel);

        MqttGatewayProperties properties = new MqttGatewayProperties();
        properties.setOutboundTopicTemplate("devices/p2p/{macRaw}");
        properties.setCompatibilityOutboundTopics(List.of("null", "device/{clientId}/down"));

        MqttTopicResolver resolver = new MqttTopicResolver(properties);
        MqttDownlinkPublisher publisher = new MqttDownlinkPublisher(provider, resolver, properties);

        String clientId = "nulllab-AI-VOX3@@@10_20_ba_40_0a_40@@@10_20_ba_40_0a_40";
        publisher.publishToDevice(clientId, "{\"type\":\"mcp\"}");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(channel, Mockito.times(1)).send(captor.capture());

        assertEquals("devices/p2p/10_20_ba_40_0a_40", captor.getValue().getHeaders().get(MqttHeaders.TOPIC));
    }
}
