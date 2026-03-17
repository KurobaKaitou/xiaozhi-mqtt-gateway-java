package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.UUID;

@Configuration
@ConditionalOnProperty(prefix = "gateway.mqtt", name = "enabled", havingValue = "true")
public class MqttIntegrationConfig {

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttGatewayProperties properties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getServerUri()});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            options.setUserName(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            options.setPassword(properties.getPassword().toCharArray());
        }
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInboundAdapter(MqttGatewayProperties properties, MqttPahoClientFactory mqttClientFactory) {
        String clientId = properties.getClientIdPrefix() + "-in-" + UUID.randomUUID();
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory, properties.getInboundTopic());
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new org.springframework.integration.mqtt.support.DefaultPahoMessageConverter());
        adapter.setQos(properties.getQos());
        adapter.setOutputChannel(mqttInboundChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler(MqttGatewayProperties properties, MqttPahoClientFactory mqttClientFactory) {
        String clientId = properties.getClientIdPrefix() + "-out-" + UUID.randomUUID();
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId, mqttClientFactory);
        handler.setAsync(true);
        handler.setDefaultQos(properties.getQos());
        handler.setDefaultRetained(false);
        return handler;
    }
}
