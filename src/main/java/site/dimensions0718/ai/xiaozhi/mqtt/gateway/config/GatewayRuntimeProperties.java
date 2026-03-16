package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway.runtime")
public class GatewayRuntimeProperties {

    private String udpPublicHost = "mqtt.xiaozhi.me";
    private int udpPort = 8884;

}
