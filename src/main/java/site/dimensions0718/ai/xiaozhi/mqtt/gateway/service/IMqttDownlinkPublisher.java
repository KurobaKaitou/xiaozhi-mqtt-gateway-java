package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

public interface IMqttDownlinkPublisher {

    void publishToDevice(String clientId, String payloadJson);
}
