package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.WebSocketBridgeProperties;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.service.IMqttDownlinkPublisher;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.IDeviceSessionStore;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.UdpAudioFrame;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.UdpDownlinkService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WebSocketBridgeService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBridgeService.class);

    private final WebSocketBridgeProperties bridgeProperties;
    private final IDeviceSessionStore sessionStore;
    private final IMqttDownlinkPublisher mqttDownlinkPublisher;
    private final UdpDownlinkService udpDownlinkService;
    private final HttpClient httpClient;

    private final Map<String, BridgeSession> sessions = new ConcurrentHashMap<>();

    public WebSocketBridgeService(WebSocketBridgeProperties bridgeProperties, IDeviceSessionStore sessionStore, IMqttDownlinkPublisher mqttDownlinkPublisher, UdpDownlinkService udpDownlinkService) {
        this.bridgeProperties = bridgeProperties;
        this.sessionStore = sessionStore;
        this.mqttDownlinkPublisher = mqttDownlinkPublisher;
        this.udpDownlinkService = udpDownlinkService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(bridgeProperties.getConnectTimeoutMillis())).build();
    }

    public void forwardDeviceAudio(UdpAudioFrame frame) {
        if (!bridgeProperties.isEnabled()) {
            return;
        }

        List<String> chatServers = bridgeProperties.getChatServers();
        if (chatServers == null || chatServers.isEmpty() || chatServers.get(0).isBlank()) {
            return;
        }

        String clientId = frame.session().clientId();
        BridgeSession session = sessions.computeIfAbsent(clientId, key -> openSession(frame.session(), chatServers, null));
        if (session == null || session.webSocket == null) {
            return;
        }

        if (session.sequenceStart < 0) {
            session.sequenceStart = frame.header().sequence();
        }

        long timestamp = resolveTimestamp(session, frame);
        byte[] payload = frame.decryptedPayload();
        byte[] wsBinary = encodeWebSocketAudio(timestamp, payload);
        session.webSocket.sendBinary(ByteBuffer.wrap(wsBinary), true);
    }

    public void ensureBridgeSession(DeviceSession session, JSONObject deviceHelloPayload) {
        if (!bridgeProperties.isEnabled()) {
            return;
        }

        List<String> chatServers = bridgeProperties.getChatServers();
        if (chatServers == null || chatServers.isEmpty() || chatServers.get(0).isBlank()) {
            return;
        }

        sessions.computeIfAbsent(session.clientId(), key -> openSession(session, chatServers, deviceHelloPayload));
    }

    private BridgeSession openSession(DeviceSession deviceSession, List<String> chatServers, JSONObject deviceHelloPayload) {
        String server = pickServer(chatServers);
        String clientId = deviceSession.clientId();
        BridgeSession bridgeSession = new BridgeSession();
        bridgeSession.sequenceStart = -1;

        try {
            WebSocket.Listener listener = new BridgeListener(clientId);
            WebSocket.Builder builder = httpClient.newWebSocketBuilder().header("device-id", deviceSession.macAddress()).header("protocol-version", String.valueOf(bridgeProperties.getHelloVersion())).header("authorization", "Bearer " + buildAuthorizationToken(deviceSession));

            if (deviceSession.uuid() != null && !deviceSession.uuid().isBlank()) {
                builder.header("client-id", deviceSession.uuid());
            }

            extractForwardedIp(deviceSession.usernameBase64()).ifPresent(ip -> builder.header("x-forwarded-for", ip));

            WebSocket webSocket = builder.buildAsync(URI.create(server), listener).join();

            bridgeSession.webSocket = webSocket;
            webSocket.sendText(buildHelloPayload(deviceHelloPayload), true);
            log.info("websocket bridge connected: clientId={}, server={}", clientId, server);
            return bridgeSession;
        } catch (RuntimeException ex) {
            log.warn("failed to open websocket bridge for clientId={}, server={}", clientId, server, ex);
            return null;
        }
    }

    private long resolveTimestamp(BridgeSession session, UdpAudioFrame frame) {
        long rawTs = frame.header().timestamp();
        if (rawTs > 0) {
            return rawTs;
        }
        long seq = frame.header().sequence();
        long relative = (seq - session.sequenceStart) * bridgeProperties.getFrameDurationMs();
        return relative & 0xFFFF_FFFFL;
    }

    private String buildHelloPayload(JSONObject deviceHelloPayload) {
        JSONObject hello = new JSONObject();
        hello.put("type", "hello");
        hello.put("version", bridgeProperties.getHelloVersion());
        hello.put("transport", "websocket");

        JSONObject audioParams = deviceHelloPayload == null ? null : deviceHelloPayload.getJSONObject("audio_params");
        if (audioParams == null) {
            audioParams = new JSONObject();
            audioParams.put("format", "opus");
            audioParams.put("sample_rate", 16000);
            audioParams.put("channels", 1);
            audioParams.put("frame_duration", bridgeProperties.getFrameDurationMs());
        }
        hello.put("audio_params", audioParams);

        JSONObject features = deviceHelloPayload == null ? null : deviceHelloPayload.getJSONObject("features");
        if (features == null) {
            features = new JSONObject();
            features.put("mcp", true);
        }
        hello.put("features", features);
        return hello.toJSONString();
    }

    private static byte[] encodeWebSocketAudio(long timestamp, byte[] opusPayload) {
        ByteBuffer buffer = ByteBuffer.allocate(16 + opusPayload.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(0L);
        buffer.putInt((int) timestamp);
        buffer.putInt(opusPayload.length);
        buffer.put(opusPayload);
        return buffer.array();
    }

    private String pickServer(List<String> chatServers) {
        int index = ThreadLocalRandom.current().nextInt(chatServers.size());
        return chatServers.get(index);
    }

    private final class BridgeListener implements WebSocket.Listener {

        private final String clientId;
        private final StringBuilder textBuffer = new StringBuilder();

        private BridgeListener(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                handleWebSocketText(clientId, textBuffer.toString());
                textBuffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (!last) {
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }

            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            handleWebSocketBinary(clientId, bytes);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            sessions.remove(clientId);
            log.info("websocket bridge closed: clientId={}, statusCode={}, reason={}", clientId, statusCode, reason);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            sessions.remove(clientId);
            log.warn("websocket bridge error: clientId={}", clientId, error);
        }
    }

    private void handleWebSocketText(String clientId, String text) {
        String payload = text == null ? "" : text.trim();
        if (payload.isBlank()) {
            return;
        }

        if (!payload.startsWith("{")) {
            if (payload.contains("认证失败") || payload.toLowerCase().contains("auth")) {
                log.error("websocket auth failed for clientId={}, message={}", clientId, payload);
            } else {
                log.warn("ignore non-json websocket text for clientId={}, message={}", clientId, payload);
            }
            return;
        }

        try {
            JSONObject json = JSON.parseObject(payload);
            String type = json.getString("type");
            if ("hello".equals(type)) {
                return;
            }
            mqttDownlinkPublisher.publishToDevice(clientId, payload);
        } catch (RuntimeException ex) {
            log.warn("ignore invalid websocket json message for clientId={}, message={}", clientId, payload);
        }
    }

    private String buildAuthorizationToken(DeviceSession session) {
        String secret = bridgeProperties.getServerSecret();
        if (secret == null || secret.isBlank()) {
            return "test-token";
        }

        String clientId = (session.uuid() == null || session.uuid().isBlank()) ? "default-client-id" : session.uuid();
        String username = session.macAddress();
        long timestamp = System.currentTimeMillis() / 1000;
        String content = clientId + "|" + username + "|" + timestamp;

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = hmac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            String signatureBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            return signatureBase64Url + "." + timestamp;
        } catch (Exception ex) {
            log.warn("failed to build bridge authorization token, fallback to test-token", ex);
            return "test-token";
        }
    }

    private Optional<String> extractForwardedIp(String usernameBase64) {
        if (usernameBase64 == null || usernameBase64.isBlank()) {
            return Optional.empty();
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(usernameBase64), StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(decoded);
            String ip = json.getString("ip");
            if (ip == null || ip.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(ip);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void handleWebSocketBinary(String clientId, byte[] bytes) {
        if (bytes.length < 16) {
            log.debug("ignore short websocket binary, clientId={}, length={}", clientId, bytes.length);
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        buffer.position(8);
        long timestamp = Integer.toUnsignedLong(buffer.getInt());
        int payloadLength = buffer.getInt();
        if (payloadLength <= 0 || bytes.length < 16 + payloadLength) {
            log.debug("ignore malformed websocket binary, clientId={}, payloadLength={}, frameLength={}", clientId, payloadLength, bytes.length);
            return;
        }

        byte[] payload = Arrays.copyOfRange(bytes, 16, 16 + payloadLength);
        Optional<DeviceSession> maybeSession = sessionStore.findByClientId(clientId);
        if (maybeSession.isEmpty()) {
            log.warn("drop websocket binary: no session found, clientId={}", clientId);
            return;
        }

        DeviceSession session = maybeSession.get();
        boolean sent = udpDownlinkService.sendAudio(session, payload, timestamp);
        log.info("websocket binary downlink result: clientId={}, payloadBytes={}, timestamp={}, sent={}", clientId, payload.length, timestamp, sent);
    }

    private static final class BridgeSession {
        private WebSocket webSocket;
        private long sequenceStart;
    }
}
