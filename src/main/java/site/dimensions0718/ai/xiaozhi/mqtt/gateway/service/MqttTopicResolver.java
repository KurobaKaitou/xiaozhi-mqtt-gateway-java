package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.MqttGatewayProperties;

@Component
public class MqttTopicResolver {

    private final MqttGatewayProperties properties;

    public MqttTopicResolver(MqttGatewayProperties properties) {
        this.properties = properties;
    }

    public String extractClientId(String topic, String payloadJson) {
        if ("device-server".equals(topic)) {
            return extractClientIdFromPayload(payloadJson);
        }
        return extractClientIdFromInboundTopic(topic);
    }

    private String extractClientIdFromInboundTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        String[] parts = topic.split("/");
        if (parts.length < 3) {
            throw new IllegalArgumentException("topic format must be device/{clientId}/up");
        }
        if (!"device".equals(parts[0]) || !"up".equals(parts[parts.length - 1])) {
            throw new IllegalArgumentException("unsupported inbound topic format");
        }
        return parts[1];
    }

    public String buildOutboundTopic(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        String template = properties.getOutboundTopicTemplate();
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("outbound topic template must not be blank");
        }
        return template.replace("{clientId}", clientId);
    }

    private String extractClientIdFromPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank for static topic mode");
        }

        JSONObject json = JSON.parseObject(payloadJson);
        String configuredField = properties.getClientIdPayloadField();
        if (configuredField != null && !configuredField.isBlank()) {
            String configured = json.getString(configuredField);
            if (configured != null && !configured.isBlank()) {
                return configured;
            }
        }

        String clientId = json.getString("client_id");
        if (clientId == null || clientId.isBlank()) {
            clientId = json.getString("clientId");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("client_id/clientId not found in payload for static topic mode");
        }
        return clientId;
    }
}
