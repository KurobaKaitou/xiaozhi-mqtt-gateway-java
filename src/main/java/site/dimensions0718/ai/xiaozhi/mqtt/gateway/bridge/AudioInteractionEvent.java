package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import java.time.Instant;

public record AudioInteractionEvent(
        String clientId,
        String macAddress,
        long sequence,
        int payloadBytes,
        String recognizedText,
        Instant occurredAt
) {
}
