package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.device;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.WebSocketBridgeService;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.AbsMqttMessageHandler;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;

import java.util.HashMap;
import java.util.Map;

@Component
public class McpMessageHandler extends AbsMqttMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(McpMessageHandler.class);

    private final WebSocketBridgeService webSocketBridgeService;

    public McpMessageHandler(WebSocketBridgeService webSocketBridgeService) {
        this.webSocketBridgeService = webSocketBridgeService;
    }

    @Override
    protected MqttMessageType type() {
        return MqttMessageType.MCP;
    }

    @Override
    protected String handle(DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload) {
        log.info("MCP 消息处理, clientId={}, payload={}", clientId, payload);
        boolean forwarded = webSocketBridgeService.forwardDeviceControl(clientId, payload.toJSONString());
        if (forwarded) {
            return null;
        }
        return buildGoodbyePayload(payload.getString("session_id"));
    }

    private static String buildGoodbyePayload(String sessionId) {
        Map<String, Object> goodbye = new HashMap<>();
        goodbye.put("type", MqttMessageType.GOODBYE.value());
        if (sessionId != null && !sessionId.isBlank()) {
            goodbye.put("session_id", sessionId);
        }
        return JSON.toJSONString(goodbye);
    }
}
