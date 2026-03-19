package site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.device;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.WebSocketBridgeService;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.GatewayRuntimeProperties;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.handler.AbsMqttMessageHandler;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.UdpPacketHeader;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.IDeviceSessionStore;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hello消息处理器
 */
@Component
public class HelloMessageHandler extends AbsMqttMessageHandler {

    private final GatewayRuntimeProperties runtimeProperties;
    private final IDeviceSessionStore sessionStore;
    private final SecureRandom secureRandom;
    private final WebSocketBridgeService webSocketBridgeService;

    public HelloMessageHandler(IDeviceSessionStore sessionStore, WebSocketBridgeService webSocketBridgeService, GatewayRuntimeProperties runtimeProperties) {
        this.sessionStore = sessionStore;
        this.runtimeProperties = runtimeProperties;
        this.secureRandom = new SecureRandom();
        this.webSocketBridgeService = webSocketBridgeService;
    }

    @Override
    protected MqttMessageType type() {
        return MqttMessageType.HELLO;
    }

    @Override
    protected String handle(DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload) {
        Integer version = payload.getInteger("version");
        String transport = payload.getString("transport");
        if (version == null || version != 3) {
            throw new IllegalArgumentException("unsupported hello version");
        }
        if (!"udp".equals(transport)) {
            throw new IllegalArgumentException("unsupported transport");
        }

        long connectionId = ThreadLocalRandom.current().nextLong(0, 0x1_0000_0000L);
        DeviceSession session = new DeviceSession(clientId, identity.groupId(), identity.macAddress(), identity.uuid(), usernameBase64, connectionId, Instant.now());

        session.markChannelRequested();
        String sessionId = UUID.randomUUID().toString();
        byte[] key = new byte[16];
        byte[] nonce = UdpPacketHeader.buildNonceHeader(connectionId);
        secureRandom.nextBytes(key);
        session.activateUdp(sessionId, version, key, nonce);
        sessionStore.upsert(session);
        webSocketBridgeService.ensureBridgeSession(session, payload);

        Map<String, Object> udp = new HashMap<>();
        udp.put("server", runtimeProperties.getUdpPublicHost());
        udp.put("port", runtimeProperties.getUdpPort());
        udp.put("encryption", "aes-128-ctr");
        udp.put("key", toHex(key));
        udp.put("nonce", toHex(nonce));

        Map<String, Object> response = new HashMap<>();
        response.put("type", "hello");
        response.put("version", version);
        response.put("session_id", sessionId);
        response.put("transport", "udp");
        response.put("udp", udp);
        response.put("audio_params", payload.get("audio_params"));
        return JSON.toJSONString(response);
    }

    private static String toHex(byte[] data) {
        return HexFormat.of().formatHex(data);
    }
}
