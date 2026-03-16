# Phase 1 - Control Plane (MQTT + Session Core)

## Goal

Deliver a stable control plane on Java/Spring Boot with EMQX integration and in-memory session management.

## In Scope

- MQTT inbound/outbound flow via Spring Integration MQTT (or Paho-based abstraction).
- Device identity parse/validation.
- Hello/Goodbye handling and session binding.
- Online/offline status tracking.
- Basic management APIs for command relay and status query.

## Out of Scope

- Real UDP audio forwarding to downstream services.
- Production-grade distributed scaling.

## Work Breakdown

1. Define core interfaces:
   - `IDeviceSessionStore`
   - `IMqttControlService`
   - `ICommandRelayService`
2. Implement in-memory session model:
   - Key: device identity (`mac` or normalized deviceId)
   - Value: MQTT state + UDP crypto placeholders + timestamps.
3. Implement control message handlers:
   - `hello`, `goodbye`, `mcp` envelope pass-through.
4. Implement management API:
   - `POST /api/commands/{clientId}`
   - `POST /api/devices/status`
5. Add MDC log context and core metrics counters.

## Deliverables

- Compilable control-plane module.
- API contracts and example requests/responses.
- Unit tests for identity parsing, session transitions, and duplicate handling.

## Acceptance Criteria

- `hello` creates session and returns valid UDP negotiation payload.
- `goodbye` closes session cleanly.
- Duplicate `clientId` replaces old session deterministically.
- Management APIs return expected status/relay behavior.
- Logs include `deviceId/mac` and lifecycle events.

## Risks

- Topic model drift between EMQX routing and upstream expectation.
- Session races on reconnect without atomic updates.

## Exit Gate

Control plane stable under reconnect and duplicate-login test scenarios.
