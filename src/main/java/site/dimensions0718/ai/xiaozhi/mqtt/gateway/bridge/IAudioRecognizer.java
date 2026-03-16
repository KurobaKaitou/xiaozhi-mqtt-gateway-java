package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import java.util.Optional;

public interface IAudioRecognizer {

    Optional<AudioRecognitionResult> recognize(AudioRecognitionRequest request);
}
