package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.junit.jupiter.api.Test;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.MqttGatewayProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttTopicResolverTests {

    @Test
    void shouldParseClientIdAndBuildResponseTopic() {
        MqttGatewayProperties properties = new MqttGatewayProperties();
        properties.setOutboundTopicTemplate("device/{clientId}/down");
        MqttTopicResolver resolver = new MqttTopicResolver(properties);

        String clientId = resolver.extractClientId("device/lichuang-dev@@@a0_85_e3_f4_49_34@@@uuid/up", "{}");
        assertEquals("lichuang-dev@@@a0_85_e3_f4_49_34@@@uuid", clientId);
        assertEquals("device/abc/down", resolver.buildOutboundTopic("abc"));
    }

    @Test
    void shouldParseClientIdFromPayloadForStaticTopic() {
        MqttGatewayProperties properties = new MqttGatewayProperties();
        MqttTopicResolver resolver = new MqttTopicResolver(properties);

        String clientId = resolver.extractClientId("device-server",
                "{\"client_id\":\"lichuang-dev@@@a0_85_e3_f4_49_34@@@uuid\",\"type\":\"hello\"}");
        assertEquals("lichuang-dev@@@a0_85_e3_f4_49_34@@@uuid", clientId);
    }

    @Test
    void shouldRejectInvalidInboundTopic() {
        MqttTopicResolver resolver = new MqttTopicResolver(new MqttGatewayProperties());
        assertThrows(IllegalArgumentException.class, () -> resolver.extractClientId("bad/topic", "{}"));
        assertThrows(IllegalArgumentException.class, () -> resolver.extractClientId("device-server", "{}"));
    }
}
