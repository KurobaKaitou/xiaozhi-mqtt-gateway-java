package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway.bridge")
public class WebSocketBridgeProperties {

    private boolean enabled = false;
    private int helloVersion = 2;
    private int frameDurationMs = 60;
    private int connectTimeoutMillis = 5000;
    private String serverSecret = "";
    private List<String> chatServers = new ArrayList<>();

}
