package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import java.util.Locale;
import java.util.regex.Pattern;

public record DeviceIdentity(String groupId, String macAddress, String uuid) {

    private static final Pattern MAC_PATTERN = Pattern.compile("^[0-9a-f]{2}(:[0-9a-f]{2}){5}$");

    public static DeviceIdentity parseClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }

        String[] parts = clientId.split("@@@", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("clientId format must be group@@@mac@@@uuid");
        }

        String groupId = parts[0];
        String macWithUnderscore = parts[1];
        String uuid = parts[2];

        if (groupId.isBlank() || macWithUnderscore.isBlank() || uuid.isBlank()) {
            throw new IllegalArgumentException("clientId parts must not be blank");
        }

        String normalizedMac = macWithUnderscore.replace('_', ':').toLowerCase(Locale.ROOT);
        if (!MAC_PATTERN.matcher(normalizedMac).matches()) {
            throw new IllegalArgumentException("mac address is invalid in clientId");
        }

        return new DeviceIdentity(groupId, normalizedMac, uuid);
    }
}
