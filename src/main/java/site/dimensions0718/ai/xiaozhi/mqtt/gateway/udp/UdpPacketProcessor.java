package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.AesCtrCodec;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.UdpPacketHeader;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.IDeviceSessionStore;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Optional;

@Component
public class UdpPacketProcessor {

    private static final long DEFAULT_SEQUENCE_TOLERANCE = 32;

    private final IDeviceSessionStore sessionStore;
    private final IUdpAudioFrameSink frameSink;
    private final UdpMetrics metrics;
    private final UdpSequenceWindow sequenceWindow;

    @Autowired
    public UdpPacketProcessor(IDeviceSessionStore sessionStore, IUdpAudioFrameSink frameSink, UdpMetrics metrics) {
        this(sessionStore, frameSink, metrics, new UdpSequenceWindow(DEFAULT_SEQUENCE_TOLERANCE));
    }

    UdpPacketProcessor(
            IDeviceSessionStore sessionStore,
            IUdpAudioFrameSink frameSink,
            UdpMetrics metrics,
            UdpSequenceWindow sequenceWindow
    ) {
        this.sessionStore = sessionStore;
        this.frameSink = frameSink;
        this.metrics = metrics;
        this.sequenceWindow = sequenceWindow;
    }

    public UdpProcessingOutcome process(byte[] packetBytes, InetSocketAddress remoteAddress) {
        if (packetBytes == null || packetBytes.length < UdpPacketHeader.HEADER_LENGTH) {
            return drop(UdpDropReason.PACKET_TOO_SHORT);
        }

        UdpPacketHeader header = UdpPacketHeader.decode(packetBytes);
        if (header.type() != UdpPacketHeader.TYPE_AUDIO) {
            return drop(UdpDropReason.UNSUPPORTED_PACKET_TYPE);
        }
        if (!UdpPacketHeader.isCompletePacket(packetBytes)) {
            return drop(UdpDropReason.PAYLOAD_LENGTH_MISMATCH);
        }

        Optional<DeviceSession> maybeSession = sessionStore.findByConnectionId(header.connectionId());
        if (maybeSession.isEmpty()) {
            return drop(UdpDropReason.UNKNOWN_SESSION);
        }
        DeviceSession session = maybeSession.get();

        if (!sequenceWindow.accept(header.connectionId(), header.sequence())) {
            return drop(UdpDropReason.REPLAY_OR_TOO_OLD);
        }

        byte[] encryptedPayload = Arrays.copyOfRange(packetBytes, UdpPacketHeader.HEADER_LENGTH,
                UdpPacketHeader.HEADER_LENGTH + header.payloadLength());

        byte[] decrypted;
        try {
            decrypted = AesCtrCodec.decrypt(encryptedPayload, session.udpKey(), session.udpNonce());
        } catch (RuntimeException ex) {
            return drop(UdpDropReason.DECRYPT_FAILED);
        }

        MDC.put("deviceId", session.clientId());
        MDC.put("mac", session.macAddress());
        try {
            UdpAudioFrame frame = new UdpAudioFrame(session, header, decrypted, remoteAddress);
            frameSink.onFrame(frame);
            metrics.markAccepted();
            session.touch();
            return UdpProcessingOutcome.accepted(frame);
        } finally {
            MDC.remove("deviceId");
            MDC.remove("mac");
        }
    }

    private UdpProcessingOutcome drop(UdpDropReason reason) {
        metrics.markDropped(reason);
        return UdpProcessingOutcome.dropped(reason);
    }
}
