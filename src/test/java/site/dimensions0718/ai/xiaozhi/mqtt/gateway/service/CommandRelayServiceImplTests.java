package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.junit.jupiter.api.Test;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.InMemoryDeviceSessionStore;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.CommandRequest;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.CommandResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRelayServiceImplTests {

    @Test
    void shouldRejectWhenDeviceNotConnected() {
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        CommandRelayServiceImpl service = new CommandRelayServiceImpl(store);

        CommandResponse response = service.relay("device-1", new CommandRequest("mcp", null));
        assertFalse(response.success());
    }

    @Test
    void shouldAcceptMcpCommandWhenSessionExists() {
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        DeviceSession session = new DeviceSession(
                "device-1",
                "group",
                "a0:85:e3:f4:49:34",
                "uuid",
                "base64",
                100,
                Instant.now()
        );
        store.upsert(session);

        CommandRelayServiceImpl service = new CommandRelayServiceImpl(store);
        CommandResponse response = service.relay("device-1", new CommandRequest("mcp", null));

        assertTrue(response.success());
    }
}
