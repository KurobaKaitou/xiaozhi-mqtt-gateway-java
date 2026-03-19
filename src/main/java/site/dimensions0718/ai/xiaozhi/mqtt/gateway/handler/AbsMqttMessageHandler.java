package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler;

import com.alibaba.fastjson2.JSONObject;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;

public abstract class AbsMqttMessageHandler {

    /**
     * 消息类型
     *
     * @return MqttMessageType
     */
    protected abstract MqttMessageType type();

    /**
     * 对应消息类型处理器
     *
     * @param identity       设备校验信息
     * @param clientId       设备clientId
     * @param usernameBase64 经过Base64编码后的设备username
     * @param payload        设备发送的mqtt有效消息载体
     * @return 处理结果
     */
    protected abstract String handle(DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload);
}
