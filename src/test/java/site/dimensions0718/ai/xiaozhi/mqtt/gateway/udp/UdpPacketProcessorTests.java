package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import org.junit.jupiter.api.Test;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.AesCtrCodec;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.UdpPacketHeader;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.InMemoryDeviceSessionStore;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UdpPacketProcessorTests {

    private static DeviceSession sessionWithActiveUdp(String clientId, long connectionId, byte[] key, byte[] nonce) {
        DeviceSession session = new DeviceSession(
                clientId,
                "group",
                "a0:85:e3:f4:49:34",
                "uuid",
                "base64",
                connectionId,
                Instant.now()
        );
        session.activateUdp("session", 3, key, nonce);
        return session;
    }

    private static byte[] packet(long connectionId, long timestamp, long sequence, byte[] encryptedPayload) {
        UdpPacketHeader header = new UdpPacketHeader(0x01, 0x00, encryptedPayload.length, connectionId, timestamp, sequence);
        byte[] headerBytes = header.encode();
        byte[] packet = Arrays.copyOf(headerBytes, headerBytes.length + encryptedPayload.length);
        System.arraycopy(encryptedPayload, 0, packet, headerBytes.length, encryptedPayload.length);
        return packet;
    }

    private static byte[] hex(String value) {
        int length = value.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return result;
    }

    @Test
    void shouldAcceptValidEncryptedPacketAndForwardFrame() {
        byte[] key = hex("00112233445566778899aabbccddeeff");
        byte[] nonce = hex("0102030405060708090a0b0c0d0e0f10");
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        DeviceSession session = sessionWithActiveUdp("device-1", 0x11223344L, key, nonce);
        store.upsert(session);

        List<UdpAudioFrame> frames = new ArrayList<>();
        UdpPacketProcessor processor = new UdpPacketProcessor(store, frames::add, new UdpMetrics(), new UdpSequenceWindow(32));

        byte[] plaintext = "opus-frame".getBytes();
        byte[] ciphertext = AesCtrCodec.encrypt(plaintext, key, nonce);
        byte[] packet = packet(0x11223344L, 100, 10, ciphertext);

        UdpProcessingOutcome outcome = processor.process(packet, new InetSocketAddress("127.0.0.1", 30000));
        assertTrue(outcome.accepted());
        assertEquals(1, frames.size());
        assertArrayEquals(plaintext, frames.get(0).decryptedPayload());
    }

    @Test
    void shouldDropWhenSessionMissing() {
        UdpPacketProcessor processor = new UdpPacketProcessor(
                new InMemoryDeviceSessionStore(),
                frame -> {
                },
                new UdpMetrics(),
                new UdpSequenceWindow(32)
        );

        byte[] packet = packet(0x99887766L, 100, 1, new byte[]{0x01, 0x02});
        UdpProcessingOutcome outcome = processor.process(packet, new InetSocketAddress("127.0.0.1", 20000));

        assertFalse(outcome.accepted());
        assertEquals(UdpDropReason.UNKNOWN_SESSION, outcome.dropReason());
    }

    @Test
    void shouldDropReplayPacket() {
        byte[] key = hex("00112233445566778899aabbccddeeff");
        byte[] nonce = hex("0102030405060708090a0b0c0d0e0f10");
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        store.upsert(sessionWithActiveUdp("device-2", 0x55667788L, key, nonce));

        UdpPacketProcessor processor = new UdpPacketProcessor(store, frame -> {
        }, new UdpMetrics(), new UdpSequenceWindow(32));
        byte[] payload = AesCtrCodec.encrypt("abc".getBytes(), key, nonce);

        UdpProcessingOutcome first = processor.process(packet(0x55667788L, 100, 10, payload),
                new InetSocketAddress("127.0.0.1", 20000));
        UdpProcessingOutcome replay = processor.process(packet(0x55667788L, 101, 10, payload),
                new InetSocketAddress("127.0.0.1", 20000));

        assertTrue(first.accepted());
        assertFalse(replay.accepted());
        assertEquals(UdpDropReason.REPLAY_OR_TOO_OLD, replay.dropReason());
    }

    @Test
    void shouldDropMalformedPacket() {
        UdpPacketProcessor processor = new UdpPacketProcessor(
                new InMemoryDeviceSessionStore(),
                frame -> {
                },
                new UdpMetrics(),
                new UdpSequenceWindow(32)
        );

        UdpProcessingOutcome outcome = processor.process(new byte[5], new InetSocketAddress("127.0.0.1", 20000));
        assertFalse(outcome.accepted());
        assertEquals(UdpDropReason.PACKET_TOO_SHORT, outcome.dropReason());
    }
}
