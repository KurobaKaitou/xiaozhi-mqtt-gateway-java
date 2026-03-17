package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.bridge")
public class WebSocketBridgeProperties {

    private boolean enabled = false;
    private int helloVersion = 2;
    private int frameDurationMs = 60;
    private int connectTimeoutMillis = 5000;
    private List<String> chatServers = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getHelloVersion() {
        return helloVersion;
    }

    public void setHelloVersion(int helloVersion) {
        this.helloVersion = helloVersion;
    }

    public int getFrameDurationMs() {
        return frameDurationMs;
    }

    public void setFrameDurationMs(int frameDurationMs) {
        this.frameDurationMs = frameDurationMs;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public List<String> getChatServers() {
        return chatServers;
    }

    public void setChatServers(List<String> chatServers) {
        this.chatServers = chatServers;
    }
}
