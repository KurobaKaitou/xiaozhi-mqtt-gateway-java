package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

public enum UdpDropReason {
    PACKET_TOO_SHORT,
    PAYLOAD_LENGTH_MISMATCH,
    UNSUPPORTED_PACKET_TYPE,
    UNKNOWN_SESSION,
    REPLAY_OR_TOO_OLD,
    DECRYPT_FAILED
}
