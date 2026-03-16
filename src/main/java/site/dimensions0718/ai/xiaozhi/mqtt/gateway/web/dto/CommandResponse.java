package site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto;

public record CommandResponse(boolean success, Object data, String error) {

    public static CommandResponse success(Object data) {
        return new CommandResponse(true, data, null);
    }

    public static CommandResponse failure(String error) {
        return new CommandResponse(false, null, error);
    }
}
