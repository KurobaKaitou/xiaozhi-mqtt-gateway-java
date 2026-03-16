package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway.udp")
public class UdpServerProperties {

    private boolean enabled = false;
    private String bindHost = "0.0.0.0";
    private int bindPort = 8884;
    private int workerThreads = 0;

}
