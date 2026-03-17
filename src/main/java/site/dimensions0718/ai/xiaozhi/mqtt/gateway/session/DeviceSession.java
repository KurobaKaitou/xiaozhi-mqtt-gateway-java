package site.dimensions0718.ai.xiaozhi.mqtt.gateway.session;

import java.time.Instant;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;

public final class DeviceSession {

    private final String clientId;
    private final String groupId;
    private final String macAddress;
    private final String uuid;
    private final String usernameBase64;
    private final long connectionId;
    private final Instant createdAt;

    private volatile Instant lastSeenAt;
    private volatile SessionState state;
    private volatile String sessionId;
    private volatile int protocolVersion;
    private volatile byte[] udpKey;
    private volatile byte[] udpNonce;
    private volatile InetSocketAddress udpRemoteAddress;
    private volatile long udpLocalSequence;

    public DeviceSession(
            String clientId,
            String groupId,
            String macAddress,
            String uuid,
            String usernameBase64,
            long connectionId,
            Instant createdAt
    ) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.macAddress = Objects.requireNonNull(macAddress, "macAddress");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.usernameBase64 = Objects.requireNonNull(usernameBase64, "usernameBase64");
        this.connectionId = connectionId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.lastSeenAt = createdAt;
        this.state = SessionState.MQTT_CONNECTED;
        this.udpLocalSequence = 0;
    }

    public synchronized void activateUdp(String sessionId, int protocolVersion, byte[] udpKey, byte[] udpNonce) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.protocolVersion = protocolVersion;
        this.udpKey = Arrays.copyOf(Objects.requireNonNull(udpKey, "udpKey"), udpKey.length);
        this.udpNonce = Arrays.copyOf(Objects.requireNonNull(udpNonce, "udpNonce"), udpNonce.length);
        this.state = SessionState.UDP_ACTIVE;
        this.lastSeenAt = Instant.now();
        this.udpLocalSequence = 0;
    }

    public synchronized void updateUdpRemoteAddress(InetSocketAddress remoteAddress) {
        this.udpRemoteAddress = remoteAddress;
        this.lastSeenAt = Instant.now();
    }

    public synchronized long nextUdpLocalSequence() {
        this.udpLocalSequence += 1;
        return this.udpLocalSequence;
    }

    public synchronized void markChannelRequested() {
        this.state = SessionState.CHANNEL_REQUESTED;
        this.lastSeenAt = Instant.now();
    }

    public synchronized void close() {
        this.state = SessionState.CLOSING;
        this.lastSeenAt = Instant.now();
    }

    public synchronized void touch() {
        this.lastSeenAt = Instant.now();
    }

    public boolean isAlive() {
        return state == SessionState.MQTT_CONNECTED || state == SessionState.CHANNEL_REQUESTED || state == SessionState.UDP_ACTIVE;
    }

    public String clientId() {
        return clientId;
    }

    public String groupId() {
        return groupId;
    }

    public String macAddress() {
        return macAddress;
    }

    public String uuid() {
        return uuid;
    }

    public String usernameBase64() {
        return usernameBase64;
    }

    public long connectionId() {
        return connectionId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastSeenAt() {
        return lastSeenAt;
    }

    public SessionState state() {
        return state;
    }

    public String sessionId() {
        return sessionId;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public byte[] udpKey() {
        return udpKey == null ? null : Arrays.copyOf(udpKey, udpKey.length);
    }

    public byte[] udpNonce() {
        return udpNonce == null ? null : Arrays.copyOf(udpNonce, udpNonce.length);
    }

    public InetSocketAddress udpRemoteAddress() {
        return udpRemoteAddress;
    }
}
