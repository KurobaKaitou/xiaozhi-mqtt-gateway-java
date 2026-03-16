# xiaozhi-mqtt-gateway-java Specs

## Intent

Build a Java + Spring Boot + Netty gateway that replaces `xiaozhi-mqtt-gateway` while keeping device-facing protocol compatibility and adding a clean path for business integrations.

## Execution Order

1. `specs/phase-0-protocol-contract.md`
2. `specs/phase-0-execution.md`
3. `specs/phase-1-control-plane.md`
4. `specs/phase-2-udp-audio-plane.md`
5. `specs/phase-3-bridge-and-business.md`

## Delivery Rule

- Do not start next phase before current phase acceptance criteria pass.
- Every phase must include logs, metrics baseline, and rollback plan.
- Keep first version single-node in-memory sessions; Redis is a later enhancement.

## Global Non-Functional Baseline

- Fail-fast for malformed packets/messages.
- No blocking work on Netty EventLoop.
- MDC log context must include `deviceId` or `mac`.
- Preserve protocol invariants from upstream behavior.
