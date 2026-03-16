package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.springframework.stereotype.Service;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.IDeviceSessionStore;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.CommandRequest;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.CommandResponse;

import java.util.Map;

@Service
public class CommandRelayServiceImpl implements ICommandRelayService {

    private final IDeviceSessionStore sessionStore;

    public CommandRelayServiceImpl(IDeviceSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public CommandResponse relay(String clientId, CommandRequest commandRequest) {
        if (commandRequest == null || commandRequest.type() == null || commandRequest.type().isBlank()) {
            return CommandResponse.failure("invalid command type");
        }

        if (sessionStore.findByClientId(clientId).isEmpty()) {
            return CommandResponse.failure("device not connected");
        }

        if (!"mcp".equals(commandRequest.type())) {
            return CommandResponse.failure("unsupported command type: " + commandRequest.type());
        }

        return CommandResponse.success(Map.of(
                "relay", "accepted",
                "clientId", clientId,
                "type", commandRequest.type()
        ));
    }
}
