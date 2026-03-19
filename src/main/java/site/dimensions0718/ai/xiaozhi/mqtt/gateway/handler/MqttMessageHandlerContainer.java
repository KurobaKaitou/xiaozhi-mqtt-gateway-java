package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;

import java.util.HashMap;
import java.util.Map;

@Component
public class MqttMessageHandlerContainer implements InitializingBean {

    private final ApplicationContext applicationContext;
    private final Map<MqttMessageType, AbsMqttMessageHandler> mqttMessageTypeEnumAbsMqttMessageHandlerMap = new HashMap<>();

    public MqttMessageHandlerContainer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, AbsMqttMessageHandler> beanMap = applicationContext.getBeansOfType(AbsMqttMessageHandler.class);
        beanMap.values().forEach(handler -> this.mqttMessageTypeEnumAbsMqttMessageHandlerMap.put(handler.type(), handler));
    }

    public AbsMqttMessageHandler getHandler(MqttMessageType messageType) {
        return this.mqttMessageTypeEnumAbsMqttMessageHandlerMap.get(messageType);
    }
}
