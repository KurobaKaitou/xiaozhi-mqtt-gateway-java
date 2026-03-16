package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import org.junit.jupiter.api.Test;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.UdpPacketHeader;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.UdpAudioFrame;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioBridgeUdpFrameSinkTests {

    private static UdpAudioFrame sampleFrame() {
        DeviceSession session = new DeviceSession(
                "client-1",
                "group",
                "a0:85:e3:f4:49:34",
                "uuid",
                "base64",
                100,
                Instant.now()
        );
        session.activateUdp("session", 3, new byte[16], new byte[16]);

        UdpPacketHeader header = new UdpPacketHeader(0x01, 0x00, 4, 100, 1, 1);
        return new UdpAudioFrame(session, header, new byte[]{0x01, 0x02, 0x03, 0x04}, new InetSocketAddress("127.0.0.1", 8884));
    }

    @Test
    void shouldPublishBusinessEventAndPushRtc() {
        Executor directExecutor = Runnable::run;

        AtomicInteger rtcPushCount = new AtomicInteger();
        AtomicBoolean eventPublished = new AtomicBoolean(false);

        IAudioRecognizer recognizer = request -> Optional.of(new AudioRecognitionResult("hello", 0.95));
        IAudioSynthesizer synthesizer = request -> Optional.of(new AudioSynthesisResult(new byte[]{0x01}, "opus", 16000));
        IRtcPusher rtcPusher = frame -> rtcPushCount.incrementAndGet();
        IBusinessEventPublisher publisher = event -> eventPublished.set(true);

        AudioBridgeUdpFrameSink sink = new AudioBridgeUdpFrameSink(
                directExecutor,
                recognizer,
                synthesizer,
                rtcPusher,
                publisher
        );

        sink.onFrame(sampleFrame());

        assertEquals(1, rtcPushCount.get());
        assertTrue(eventPublished.get());
    }

    @Test
    void shouldKeepFlowWhenRecognizerReturnsEmpty() {
        Executor directExecutor = Runnable::run;
        AtomicInteger eventCount = new AtomicInteger();

        IAudioRecognizer recognizer = request -> Optional.empty();
        IAudioSynthesizer synthesizer = request -> {
            throw new AssertionError("synthesizer should not be called for empty text");
        };
        IRtcPusher rtcPusher = frame -> {
        };
        IBusinessEventPublisher publisher = event -> eventCount.incrementAndGet();

        AudioBridgeUdpFrameSink sink = new AudioBridgeUdpFrameSink(
                directExecutor,
                recognizer,
                synthesizer,
                rtcPusher,
                publisher
        );

        sink.onFrame(sampleFrame());
        assertEquals(1, eventCount.get());
    }
}
