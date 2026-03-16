package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

public record UdpProcessingOutcome(boolean accepted, UdpAudioFrame frame, UdpDropReason dropReason) {

    public static UdpProcessingOutcome accepted(UdpAudioFrame frame) {
        return new UdpProcessingOutcome(true, frame, null);
    }

    public static UdpProcessingOutcome dropped(UdpDropReason reason) {
        return new UdpProcessingOutcome(false, null, reason);
    }
}
