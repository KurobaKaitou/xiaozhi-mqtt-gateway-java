package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MqttMessageTypeTests {

    @Test
    void shouldParseKnownTypesCaseInsensitively() {
        assertEquals(MqttMessageType.HELLO, MqttMessageType.from("hello"));
        assertEquals(MqttMessageType.GOODBYE, MqttMessageType.from("GOODBYE"));
        assertEquals(MqttMessageType.MCP, MqttMessageType.from("Mcp"));
    }

    @Test
    void shouldReturnUnknownForInvalidType() {
        assertEquals(MqttMessageType.UNKNOWN, MqttMessageType.from(null));
        assertEquals(MqttMessageType.UNKNOWN, MqttMessageType.from(""));
        assertEquals(MqttMessageType.UNKNOWN, MqttMessageType.from("unknown_type"));
    }
}
