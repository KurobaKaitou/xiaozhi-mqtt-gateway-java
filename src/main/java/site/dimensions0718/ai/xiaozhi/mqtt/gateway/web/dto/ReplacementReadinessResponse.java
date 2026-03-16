package site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto;

import java.util.List;

public record ReplacementReadinessResponse(boolean ready, List<ReadinessCheckItem> checks) {
}
