package site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto;

public record ReadinessCheckItem(String code, boolean passed, String detail, boolean critical) {
}
