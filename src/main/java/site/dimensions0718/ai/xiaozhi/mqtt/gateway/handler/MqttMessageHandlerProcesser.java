package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;

@Component
public class MqttMessageHandlerProcesser {

    private final MqttMessageHandlerContainer mqttMessageHandlerContainer;

    public MqttMessageHandlerProcesser(MqttMessageHandlerContainer mqttMessageHandlerContainer) {
        this.mqttMessageHandlerContainer = mqttMessageHandlerContainer;
    }

    public String handle(MqttMessageType messageType, DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload) {
        AbsMqttMessageHandler handler = mqttMessageHandlerContainer.getHandler(messageType);
        if (handler == null) {
            throw new RuntimeException("没有找到对应的处理器");
        }
        return handler.handle(identity, clientId, usernameBase64, payload);
    }
}
