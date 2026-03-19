package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.device;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.WebSocketBridgeService;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.AbsMqttMessageHandler;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.IDeviceSessionStore;

/**
 * Goodbye消息处理器
 */
@Component
public class GoodByeMessageHandler extends AbsMqttMessageHandler {
    private final IDeviceSessionStore sessionStore;
    private final WebSocketBridgeService webSocketBridgeService;

    public GoodByeMessageHandler(IDeviceSessionStore sessionStore, WebSocketBridgeService webSocketBridgeService) {
        this.sessionStore = sessionStore;
        this.webSocketBridgeService = webSocketBridgeService;
    }

    @Override
    protected MqttMessageType type() {
        return MqttMessageType.GOODBYE;
    }

    @Override
    protected String handle(DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload) {
        webSocketBridgeService.closeBridgeSession(clientId, "device_goodbye");
        sessionStore.removeByClientId(clientId);
        return null;
    }
}
