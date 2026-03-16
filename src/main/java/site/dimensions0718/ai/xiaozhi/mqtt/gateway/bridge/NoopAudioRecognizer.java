package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import java.util.Optional;

public class NoopAudioRecognizer implements IAudioRecognizer {

    @Override
    public Optional<AudioRecognitionResult> recognize(AudioRecognitionRequest request) {
        return Optional.empty();
    }
}
