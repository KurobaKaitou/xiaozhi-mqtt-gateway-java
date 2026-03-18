package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.springframework.stereotype.Service;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.CommandType;
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
        if (commandRequest == null) {
            return CommandResponse.failure("invalid command type");
        }

        CommandType commandType = CommandType.from(commandRequest.type());
        if (commandType == CommandType.UNKNOWN) {
            return CommandResponse.failure("invalid command type");
        }

        if (sessionStore.findByClientId(clientId).isEmpty()) {
            return CommandResponse.failure("device not connected");
        }

        if (commandType != CommandType.MCP) {
            return CommandResponse.failure("unsupported command type: " + commandRequest.type());
        }

        return CommandResponse.success(Map.of(
                "relay", "accepted",
                "clientId", clientId,
                "type", commandType.name().toLowerCase()
        ));
    }
}
