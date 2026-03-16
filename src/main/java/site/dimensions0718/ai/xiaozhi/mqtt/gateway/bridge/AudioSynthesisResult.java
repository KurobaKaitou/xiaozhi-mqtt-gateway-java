package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

public record AudioSynthesisResult(byte[] audioBytes, String format, int sampleRate) {
}
