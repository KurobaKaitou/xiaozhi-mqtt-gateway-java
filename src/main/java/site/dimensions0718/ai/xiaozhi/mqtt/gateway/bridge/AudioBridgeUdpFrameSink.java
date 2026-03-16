package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.IUdpAudioFrameSink;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.UdpAudioFrame;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;

@Component
@Primary
public class AudioBridgeUdpFrameSink implements IUdpAudioFrameSink {

    private static final Logger log = LoggerFactory.getLogger(AudioBridgeUdpFrameSink.class);

    private final Executor audioBridgeExecutor;
    private final IAudioRecognizer audioRecognizer;
    private final IAudioSynthesizer audioSynthesizer;
    private final IRtcPusher rtcPusher;
    private final IBusinessEventPublisher businessEventPublisher;

    public AudioBridgeUdpFrameSink(
            @Qualifier("audioBridgeExecutor") Executor audioBridgeExecutor,
            IAudioRecognizer audioRecognizer,
            IAudioSynthesizer audioSynthesizer,
            IRtcPusher rtcPusher,
            IBusinessEventPublisher businessEventPublisher
    ) {
        this.audioBridgeExecutor = audioBridgeExecutor;
        this.audioRecognizer = audioRecognizer;
        this.audioSynthesizer = audioSynthesizer;
        this.rtcPusher = rtcPusher;
        this.businessEventPublisher = businessEventPublisher;
    }

    @Override
    public void onFrame(UdpAudioFrame frame) {
        audioBridgeExecutor.execute(() -> processFrame(frame));
    }

    private void processFrame(UdpAudioFrame frame) {
        try {
            rtcPusher.push(frame);

            Optional<AudioRecognitionResult> recognition = audioRecognizer.recognize(new AudioRecognitionRequest(frame));
            String text = recognition.map(AudioRecognitionResult::text).orElse("");

            if (!text.isBlank()) {
                audioSynthesizer.synthesize(new AudioSynthesisRequest(text));
            }

            businessEventPublisher.publishAudioInteraction(new AudioInteractionEvent(
                    frame.session().clientId(),
                    frame.session().macAddress(),
                    frame.header().sequence(),
                    frame.decryptedPayload().length,
                    text,
                    Instant.now()
            ));
        } catch (RuntimeException ex) {
            log.warn("audio bridge processing failed for clientId={}", frame.session().clientId(), ex);
        }
    }
}
