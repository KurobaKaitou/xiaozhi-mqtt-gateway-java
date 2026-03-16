package site.dimensions0718.ai.xiaozhi.mqtt.gateway.session;

public enum SessionState {
    DISCONNECTED,
    MQTT_CONNECTED,
    CHANNEL_REQUESTED,
    UDP_ACTIVE,
    CLOSING
}
