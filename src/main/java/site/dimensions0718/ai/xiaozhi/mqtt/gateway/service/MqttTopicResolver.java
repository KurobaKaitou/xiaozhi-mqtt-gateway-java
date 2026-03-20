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
        if (isDevicesP2pTopic(topic)) {
            return extractClientIdFromDevicesP2pTopic(topic, payloadJson);
        }
        if (isDeviceServerTopic(topic)) {
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
            throw new IllegalArgumentException("topic format must be device/{clientId}/up or devices/p2p/{macRaw}");
        }
        if (!"device".equals(parts[0]) || !"up".equals(parts[parts.length - 1])) {
            throw new IllegalArgumentException("unsupported inbound topic format");
        }
        return parts[1];
    }

    private String extractClientIdFromDevicesP2pTopic(String topic, String payloadJson) {
        String[] parts = topic.split("/");
        if (parts.length != 3 || !"devices".equals(parts[0]) || !"p2p".equals(parts[1]) || parts[2].isBlank()) {
            throw new IllegalArgumentException("topic format must be devices/p2p/{macRaw}");
        }

        String clientId = extractClientIdFromPayload(payloadJson);
        String topicMacRaw = parts[2];
        String payloadMacRaw = extractMacRawFromClientId(clientId);
        if (!normalizeMacRaw(topicMacRaw).equals(normalizeMacRaw(payloadMacRaw))) {
            throw new IllegalArgumentException("clientId mac segment mismatches devices/p2p topic");
        }
        return clientId;
    }

    public String buildOutboundTopic(String clientId) {
        return resolveTopicTemplate(properties.getOutboundTopicTemplate(), clientId);
    }

    public String resolveTopicTemplate(String template, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("outbound topic template must not be blank");
        }
        String macRaw = extractMacRawFromClientId(clientId);
        return template
                .replace("{clientId}", clientId)
                .replace("{macRaw}", macRaw);
    }

    private static String extractMacRawFromClientId(String clientId) {
        String[] parts = clientId.split("@@@", -1);
        if (parts.length >= 2 && !parts[1].isBlank()) {
            return parts[1];
        }
        return clientId;
    }

    private static boolean isDevicesP2pTopic(String topic) {
        return topic != null && topic.startsWith("devices/p2p/");
    }

    private static boolean isDeviceServerTopic(String topic) {
        return topic != null && ("device-server".equals(topic) || topic.startsWith("device-server/"));
    }

    private static String normalizeMacRaw(String macRaw) {
        return macRaw.replace(':', '_').toLowerCase();
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
