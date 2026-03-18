package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import java.util.Locale;

public enum CommandType {
    MCP,
    UNKNOWN;

    public static CommandType from(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return UNKNOWN;
        }
        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        return "MCP".equals(normalized) ? MCP : UNKNOWN;
    }
}
