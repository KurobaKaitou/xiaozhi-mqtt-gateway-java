package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceIdentityTests {

    @Test
    void shouldParseClientIdAndNormalizeMac() {
        DeviceIdentity identity = DeviceIdentity.parseClientId(
                "lichuang-dev@@@A0_85_E3_F4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932"
        );

        assertEquals("lichuang-dev", identity.groupId());
        assertEquals("a0:85:e3:f4:49:34", identity.macAddress());
        assertEquals("aeebef32-f0ef-4bce-9d8a-894d91bc6932", identity.uuid());
    }

    @Test
    void shouldRejectInvalidClientId() {
        assertThrows(IllegalArgumentException.class, () -> DeviceIdentity.parseClientId("only@@@two"));
        assertThrows(IllegalArgumentException.class, () -> DeviceIdentity.parseClientId("g@@@bad_mac@@@u"));
    }
}
