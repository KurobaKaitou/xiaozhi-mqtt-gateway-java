package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;

@Component
public class MqttMessageHandlerProcesser {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageHandlerProcesser.class);

    private final MqttMessageHandlerContainer mqttMessageHandlerContainer;

    public MqttMessageHandlerProcesser(MqttMessageHandlerContainer mqttMessageHandlerContainer) {
        this.mqttMessageHandlerContainer = mqttMessageHandlerContainer;
    }

    public String handle(MqttMessageType messageType, DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload) {
        AbsMqttMessageHandler handler = mqttMessageHandlerContainer.getHandler(messageType);
        if (handler == null) {
            log.warn("skip unsupported mqtt message type: type={}, clientId={}", messageType, clientId);
            return null;
        }
        return handler.handle(identity, clientId, usernameBase64, payload);
    }
}
