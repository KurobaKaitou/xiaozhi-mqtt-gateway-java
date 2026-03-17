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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public WebSocketBridgeService(
            WebSocketBridgeProperties bridgeProperties,
            IDeviceSessionStore sessionStore,
            IMqttDownlinkPublisher mqttDownlinkPublisher,
            UdpDownlinkService udpDownlinkService
    ) {
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
        BridgeSession session = sessions.computeIfAbsent(clientId, key -> openSession(frame, chatServers));
        if (session == null || session.webSocket == null) {
            return;
        }

        long timestamp = resolveTimestamp(session, frame);
        byte[] payload = frame.decryptedPayload();
        byte[] wsBinary = encodeWebSocketAudio(timestamp, payload);
        session.webSocket.sendBinary(ByteBuffer.wrap(wsBinary), true);
    }

    private BridgeSession openSession(UdpAudioFrame frame, List<String> chatServers) {
        String server = pickServer(chatServers);
        DeviceSession deviceSession = frame.session();
        String clientId = deviceSession.clientId();
        BridgeSession bridgeSession = new BridgeSession();
        bridgeSession.sequenceStart = frame.header().sequence();
        bridgeSession.startAt = Instant.now();

        try {
            WebSocket.Listener listener = new BridgeListener(clientId);
            WebSocket webSocket = httpClient.newWebSocketBuilder()
                    .header("device-id", deviceSession.macAddress())
                    .header("protocol-version", String.valueOf(bridgeProperties.getHelloVersion()))
                    .buildAsync(URI.create(server), listener)
                    .join();

            bridgeSession.webSocket = webSocket;
            webSocket.sendText(buildHelloPayload(), true);
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

    private String buildHelloPayload() {
        JSONObject hello = new JSONObject();
        hello.put("type", "hello");
        hello.put("version", bridgeProperties.getHelloVersion());
        hello.put("transport", "websocket");

        JSONObject audioParams = new JSONObject();
        audioParams.put("format", "opus");
        audioParams.put("sample_rate", 16000);
        audioParams.put("channels", 1);
        audioParams.put("frame_duration", bridgeProperties.getFrameDurationMs());
        hello.put("audio_params", audioParams);

        JSONObject features = new JSONObject();
        features.put("mcp", true);
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
        try {
            JSONObject json = JSON.parseObject(text);
            String type = json.getString("type");
            if ("hello".equals(type)) {
                return;
            }
            mqttDownlinkPublisher.publishToDevice(clientId, text);
        } catch (RuntimeException ex) {
            log.warn("ignore invalid websocket text message for clientId={}", clientId, ex);
        }
    }

    private void handleWebSocketBinary(String clientId, byte[] bytes) {
        if (bytes.length < 16) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        buffer.position(8);
        long timestamp = Integer.toUnsignedLong(buffer.getInt());
        int payloadLength = buffer.getInt();
        if (payloadLength <= 0 || bytes.length < 16 + payloadLength) {
            return;
        }

        byte[] payload = Arrays.copyOfRange(bytes, 16, 16 + payloadLength);
        Optional<DeviceSession> maybeSession = sessionStore.findByClientId(clientId);
        maybeSession.ifPresent(session -> udpDownlinkService.sendAudio(session, payload, timestamp));
    }

    private static final class BridgeSession {
        private WebSocket webSocket;
        private long sequenceStart;
        private Instant startAt;
    }
}
