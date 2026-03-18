package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GatewayStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(GatewayStartupLogger.class);

    private final MqttGatewayProperties mqttGatewayProperties;
    private final UdpServerProperties udpServerProperties;
    private final WebSocketBridgeProperties webSocketBridgeProperties;

    public GatewayStartupLogger(
            MqttGatewayProperties mqttGatewayProperties,
            UdpServerProperties udpServerProperties,
            WebSocketBridgeProperties webSocketBridgeProperties
    ) {
        this.mqttGatewayProperties = mqttGatewayProperties;
        this.udpServerProperties = udpServerProperties;
        this.webSocketBridgeProperties = webSocketBridgeProperties;
    }

    @PostConstruct
    public void logStartupConfiguration() {
        log.info("gateway startup config: mqtt.enabled={}, mqtt.serverUri={}, mqtt.inboundTopic={}, mqtt.outboundTopicTemplate={}, mqtt.compatTopics={} ",
                mqttGatewayProperties.isEnabled(),
                mqttGatewayProperties.getServerUri(),
                mqttGatewayProperties.getInboundTopic(),
                mqttGatewayProperties.getOutboundTopicTemplate(),
                mqttGatewayProperties.getCompatibilityOutboundTopics());

        log.info("gateway startup config: udp.enabled={}, udp.bind={}:{}, udp.publicHost={} ",
                udpServerProperties.isEnabled(),
                udpServerProperties.getBindHost(),
                udpServerProperties.getBindPort(),
                "(from gateway.runtime.udp-public-host)");

        log.info("gateway startup config: bridge.enabled={}, bridge.chatServers={}, bridge.helloVersion={}, bridge.frameDurationMs={} ",
                webSocketBridgeProperties.isEnabled(),
                webSocketBridgeProperties.getChatServers(),
                webSocketBridgeProperties.getHelloVersion(),
                webSocketBridgeProperties.getFrameDurationMs());
    }
}
