package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

public interface IMqttControlService {

    String handlePublish(String clientId, String usernameBase64, String password, String payloadJson);

    String handleBrokerPublish(String clientId, String payloadJson);
}
