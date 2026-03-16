package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.UdpAudioFrame;

public record AudioRecognitionRequest(UdpAudioFrame frame) {
}
