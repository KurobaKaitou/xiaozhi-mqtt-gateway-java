package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.MqttMessageHandlerProcesser;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttCredentialSignature;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;

@Service
public class MqttControlServiceImpl implements IMqttControlService {

    private final MqttMessageHandlerProcesser mqttMessageHandlerProcesser;
    private final String signatureKey;

    @Autowired
    public MqttControlServiceImpl(MqttMessageHandlerProcesser mqttMessageHandlerProcesser, org.springframework.core.env.Environment environment) {
        this(mqttMessageHandlerProcesser, environment.getProperty("MQTT_SIGNATURE_KEY", ""));
    }

    MqttControlServiceImpl(MqttMessageHandlerProcesser mqttMessageHandlerProcesser, String signatureKey) {
        this.mqttMessageHandlerProcesser = mqttMessageHandlerProcesser;
        this.signatureKey = signatureKey == null ? "" : signatureKey;
    }


    @Override
    public String handlePublish(String clientId, String usernameBase64, String password, String payloadJson) {
        DeviceIdentity identity = DeviceIdentity.parseClientId(clientId);
        verifySignature(clientId, usernameBase64, password);

        return handlePayload(identity, clientId, usernameBase64, payloadJson);
    }

    @Override
    public String handleBrokerPublish(String clientId, String payloadJson) {
        DeviceIdentity identity = DeviceIdentity.parseClientId(clientId);
        return handlePayload(identity, clientId, "broker", payloadJson);
    }

    private void verifySignature(String clientId, String usernameBase64, String password) {
        if (signatureKey.isBlank()) {
            throw new IllegalStateException("MQTT_SIGNATURE_KEY is not configured");
        }
        if (!MqttCredentialSignature.verify(clientId, usernameBase64, signatureKey, password)) {
            throw new IllegalArgumentException("invalid mqtt credential signature");
        }
    }

    private String handlePayload(DeviceIdentity identity, String clientId, String usernameBase64, String payloadJson) {
        JSONObject payload = JSON.parseObject(payloadJson);
        MqttMessageType messageType = MqttMessageType.from(payload.getString("type"));
        return mqttMessageHandlerProcesser.handle(messageType, identity, clientId, usernameBase64, payload);
    }
}
