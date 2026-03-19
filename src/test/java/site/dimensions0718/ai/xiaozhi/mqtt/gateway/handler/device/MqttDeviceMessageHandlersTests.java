package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.device;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.service.IGpsReportService;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.WebSocketBridgeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttDeviceMessageHandlersTests {

    @Test
    void mcpReturnsNullWhenForwardedToBridge() {
        WebSocketBridgeService bridgeService = mock(WebSocketBridgeService.class);
        when(bridgeService.forwardDeviceControl(anyString(), anyString())).thenReturn(true);

        McpMessageHandler handler = new McpMessageHandler(bridgeService);
        String result = handler.handle(null, "c1", "broker", JSON.parseObject("{\"type\":\"mcp\"}"));

        assertNull(result);
    }

    @Test
    void mcpReturnsGoodbyeWhenBridgeUnavailable() {
        WebSocketBridgeService bridgeService = mock(WebSocketBridgeService.class);
        when(bridgeService.forwardDeviceControl(anyString(), anyString())).thenReturn(false);

        McpMessageHandler handler = new McpMessageHandler(bridgeService);
        String result = handler.handle(null, "c1", "broker", JSON.parseObject("{\"type\":\"mcp\",\"session_id\":\"s1\"}"));

        JSONObject response = JSON.parseObject(result);
        assertEquals("goodbye", response.getString("type"));
        assertEquals("s1", response.getString("session_id"));
    }

    @Test
    void gpsReturnsNullWhenForwardedToBridge() {
        IGpsReportService gpsReportService = mock(IGpsReportService.class);

        GpsReportMessageHandler handler = new GpsReportMessageHandler(gpsReportService);
        String result = handler.handle(DeviceIdentity.parseClientId("group@@@aa_bb_cc_dd_ee_ff@@@u"), "c1", "broker", JSON.parseObject("{\"type\":\"gps_report\",\"gps\":{\"lat\":30.123,\"lng\":120.456,\"timestamp\":1710848000}}"));

        verify(gpsReportService).report(any());
        assertNull(result);
    }

    @Test
    void gpsReportsFromPayloadNodeAndAlwaysReturnsNull() {
        IGpsReportService gpsReportService = mock(IGpsReportService.class);

        GpsReportMessageHandler handler = new GpsReportMessageHandler(gpsReportService);
        String result = handler.handle(DeviceIdentity.parseClientId("group@@@aa_bb_cc_dd_ee_ff@@@u"), "c1", "broker", JSON.parseObject("{\"type\":\"gps_report\",\"payload\":{\"latitude\":30.1,\"longitude\":120.2,\"ts\":1710848123}}"));

        verify(gpsReportService).report(any());
        assertNull(result);
    }
}
