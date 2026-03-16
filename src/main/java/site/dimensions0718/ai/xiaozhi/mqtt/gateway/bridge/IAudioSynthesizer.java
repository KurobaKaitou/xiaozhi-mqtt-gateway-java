package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import java.util.Optional;

public interface IAudioSynthesizer {

    Optional<AudioSynthesisResult> synthesize(AudioSynthesisRequest request);
}
