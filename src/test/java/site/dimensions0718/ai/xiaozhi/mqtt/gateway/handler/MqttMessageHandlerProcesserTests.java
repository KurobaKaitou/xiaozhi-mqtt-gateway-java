package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MqttMessageHandlerProcesserTests {

    @Test
    void returnsNullWhenHandlerMissing() {
        MqttMessageHandlerContainer container = mock(MqttMessageHandlerContainer.class);
        when(container.getHandler(MqttMessageType.UNKNOWN)).thenReturn(null);

        MqttMessageHandlerProcesser processer = new MqttMessageHandlerProcesser(container);
        String result = processer.handle(MqttMessageType.UNKNOWN, null, "c1", "broker", new JSONObject());

        assertNull(result);
    }

    @Test
    void delegatesToResolvedHandler() {
        MqttMessageHandlerContainer container = mock(MqttMessageHandlerContainer.class);
        AbsMqttMessageHandler handler = mock(AbsMqttMessageHandler.class);
        when(container.getHandler(MqttMessageType.MCP)).thenReturn(handler);
        when(handler.handle(any(DeviceIdentity.class), eq("c1"), eq("broker"), any(JSONObject.class))).thenReturn("ok");

        MqttMessageHandlerProcesser processer = new MqttMessageHandlerProcesser(container);
        String result = processer.handle(MqttMessageType.MCP, DeviceIdentity.parseClientId("group@@@aa_bb_cc_dd_ee_ff@@@u"), "c1", "broker", new JSONObject());

        assertEquals("ok", result);
    }
}
