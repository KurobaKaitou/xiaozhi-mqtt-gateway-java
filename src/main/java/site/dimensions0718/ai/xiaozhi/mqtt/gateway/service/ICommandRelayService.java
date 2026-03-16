package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.CommandRequest;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.CommandResponse;

public interface ICommandRelayService {

    CommandResponse relay(String clientId, CommandRequest commandRequest);
}
