package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import java.util.Locale;

public enum MqttMessageType {
    HELLO,
    GOODBYE,
    MCP,
    GPS_REPORT,
    UNKNOWN;

    public static MqttMessageType from(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return UNKNOWN;
        }
        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HELLO" -> HELLO;
            case "GOODBYE" -> GOODBYE;
            case "MCP" -> MCP;
            case "GPS_REPORT" -> GPS_REPORT;
            default -> UNKNOWN;
        };
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
