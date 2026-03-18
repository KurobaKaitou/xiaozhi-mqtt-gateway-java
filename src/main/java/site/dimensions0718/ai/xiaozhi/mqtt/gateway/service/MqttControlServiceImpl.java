package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.WebSocketBridgeService;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.GatewayRuntimeProperties;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.DeviceIdentity;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttMessageType;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttCredentialSignature;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.IDeviceSessionStore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MqttControlServiceImpl implements IMqttControlService {

    private final IDeviceSessionStore sessionStore;
    private final GatewayRuntimeProperties runtimeProperties;
    private final WebSocketBridgeService webSocketBridgeService;
    private final String signatureKey;
    private final SecureRandom secureRandom;
    private final Map<MqttMessageType, MqttMessageHandlerStrategy> messageHandlers;

    @Autowired
    public MqttControlServiceImpl(IDeviceSessionStore sessionStore, GatewayRuntimeProperties runtimeProperties, WebSocketBridgeService webSocketBridgeService, org.springframework.core.env.Environment environment) {
        this(sessionStore, runtimeProperties, webSocketBridgeService, environment.getProperty("MQTT_SIGNATURE_KEY", ""), new SecureRandom());
    }

    MqttControlServiceImpl(IDeviceSessionStore sessionStore, GatewayRuntimeProperties runtimeProperties, WebSocketBridgeService webSocketBridgeService, String signatureKey, SecureRandom secureRandom) {
        this.sessionStore = sessionStore;
        this.runtimeProperties = runtimeProperties;
        this.webSocketBridgeService = webSocketBridgeService;
        this.signatureKey = signatureKey == null ? "" : signatureKey;
        this.secureRandom = secureRandom;
        this.messageHandlers = createMessageHandlers();
    }

    private Map<MqttMessageType, MqttMessageHandlerStrategy> createMessageHandlers() {
        EnumMap<MqttMessageType, MqttMessageHandlerStrategy> handlers = new EnumMap<>(MqttMessageType.class);
        handlers.put(MqttMessageType.HELLO, context -> handleHello(context.identity(), context.clientId(), context.usernameBase64(), context.payload()));
        handlers.put(MqttMessageType.GOODBYE, context -> {
            handleGoodbye(context.clientId());
            return null;
        });
        return Map.copyOf(handlers);
    }

    private static String toHex(byte[] data) {
        return HexFormat.of().formatHex(data);
    }

    @Override
    public String handlePublish(String clientId, String usernameBase64, String password, String payloadJson) {
        DeviceIdentity identity = DeviceIdentity.parseClientId(clientId);
        verifySignature(clientId, usernameBase64, password);

        return handlePayload(identity, clientId, usernameBase64, payloadJson);
    }

    @Override
    public String handleBrokerPublish(String clientId, String payloadJson) {
        DeviceIdentity identity = DeviceIdentity.parseClientId(clientId);
        return handlePayload(identity, clientId, "broker", payloadJson);
    }

    private void verifySignature(String clientId, String usernameBase64, String password) {
        if (signatureKey.isBlank()) {
            throw new IllegalStateException("MQTT_SIGNATURE_KEY is not configured");
        }
        if (!MqttCredentialSignature.verify(clientId, usernameBase64, signatureKey, password)) {
            throw new IllegalArgumentException("invalid mqtt credential signature");
        }
    }

    private String handlePayload(DeviceIdentity identity, String clientId, String usernameBase64, String payloadJson) {
        JSONObject payload = JSON.parseObject(payloadJson);
        MqttMessageType messageType = MqttMessageType.from(payload.getString("type"));
        MqttMessageHandlerStrategy handler = messageHandlers.get(messageType);
        if (handler == null) {
            return null;
        }
        return handler.handle(new MessageHandleContext(identity, clientId, usernameBase64, payload));
    }

    private String handleHello(DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload) {
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
        byte[] nonce = buildUdpNonceHeader(connectionId);
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

    private void handleGoodbye(String clientId) {
        webSocketBridgeService.closeBridgeSession(clientId, "device_goodbye");
        sessionStore.removeByClientId(clientId);
    }

    private static byte[] buildUdpNonceHeader(long connectionId) {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);
        buffer.putShort((short) 0);
        buffer.putInt((int) connectionId);
        buffer.putInt(0);
        buffer.putInt(0);
        return buffer.array();
    }

    private record MessageHandleContext(DeviceIdentity identity, String clientId, String usernameBase64, JSONObject payload) {
    }

    @FunctionalInterface
    private interface MqttMessageHandlerStrategy {
        String handle(MessageHandleContext context);
    }
}
