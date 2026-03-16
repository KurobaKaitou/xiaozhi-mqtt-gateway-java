package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import java.util.Optional;

public class NoopAudioSynthesizer implements IAudioSynthesizer {

    @Override
    public Optional<AudioSynthesisResult> synthesize(AudioSynthesisRequest request) {
        return Optional.empty();
    }
}
