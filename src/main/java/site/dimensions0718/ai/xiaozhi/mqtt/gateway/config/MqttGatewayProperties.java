package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway.mqtt")
public class MqttGatewayProperties {

    private boolean enabled = false;
    private String serverUri;
    private String clientIdPrefix;
    private String username;
    private String password;
    private String inboundTopic;
    private String outboundTopicTemplate;
    private String clientIdPayloadField;
    private List<String> compatibilityOutboundTopics = new ArrayList<>();
    private int qos;

}
