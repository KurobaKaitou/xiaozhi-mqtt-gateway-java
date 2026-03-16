package site.dimensions0718.ai.xiaozhi.mqtt.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.service.ICommandRelayService;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.service.ReplacementReadinessService;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.IDeviceSessionStore;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DeviceManagementController {

    private final ICommandRelayService commandRelayService;
    private final IDeviceSessionStore sessionStore;
    private final ReplacementReadinessService replacementReadinessService;

    public DeviceManagementController(
            ICommandRelayService commandRelayService,
            IDeviceSessionStore sessionStore,
            ReplacementReadinessService replacementReadinessService
    ) {
        this.commandRelayService = commandRelayService;
        this.sessionStore = sessionStore;
        this.replacementReadinessService = replacementReadinessService;
    }

    @PostMapping("/commands/{clientId}")
    public Mono<ResponseEntity<CommandResponse>> relayCommand(
            @PathVariable String clientId,
            @RequestBody CommandRequest commandRequest
    ) {
        CommandResponse response = commandRelayService.relay(clientId, commandRequest);
        if (response.success()) {
            return Mono.just(ResponseEntity.ok(response));
        }
        HttpStatus status = "device not connected".equals(response.error()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return Mono.just(ResponseEntity.status(status).body(response));
    }

    @PostMapping("/devices/status")
    public Mono<ResponseEntity<Map<String, DeviceStatusItem>>> status(@RequestBody DeviceStatusRequest request) {
        List<String> clientIds = request == null || request.clientIds() == null ? List.of() : request.clientIds();
        Map<String, DeviceStatusItem> result = new HashMap<>();
        for (String clientId : clientIds) {
            DeviceStatusItem item = sessionStore.findByClientId(clientId)
                    .map(this::toStatus)
                    .orElse(new DeviceStatusItem(false, false, "DISCONNECTED"));
            result.put(clientId, item);
        }
        return Mono.just(ResponseEntity.ok(result));
    }

    private DeviceStatusItem toStatus(DeviceSession session) {
        return new DeviceStatusItem(true, session.isAlive(), session.state().name());
    }

    @PostMapping("/system/replacement-readiness")
    public Mono<ResponseEntity<ReplacementReadinessResponse>> replacementReadiness() {
        return Mono.just(ResponseEntity.ok(replacementReadinessService.evaluate()));
    }
}
