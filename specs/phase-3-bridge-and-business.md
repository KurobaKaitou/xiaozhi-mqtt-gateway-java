# Phase 3 - Bridge and Business Integrations

## Goal

Connect protocol core to AI/audio/business systems (ASR/TTS/TRTC/Spring Cloud) while preserving gateway stability.

## In Scope

- Bridge decrypted audio stream to ASR/TTS pipeline interfaces.
- Implement RTC push abstraction (`IRtcPusher`) for app-side listen/talk scenarios.
- Integrate business events (online duration, interaction counts, GPS report).
- Harden observability: metrics, tracing IDs, error dashboards.

## Out of Scope

- Full product analytics platform.
- Cross-cluster session federation (post-MVP).

## Work Breakdown

1. Define boundary interfaces:
   - `IAudioRecognizer`
   - `IAudioSynthesizer`
   - `IRtcPusher`
   - `IBusinessEventPublisher`
2. Implement adapter layer for third-party services.
3. Add retry/timeouts/circuit protections for external calls.
4. Extend management API for operational controls if needed.
5. Prepare Redis-backed `DeviceSessionStore` as optional upgrade path.

## Deliverables

- End-to-end call flow from device audio uplink to response downlink.
- DTO contracts for Spring Cloud integration.
- Operational runbook (alerts, troubleshooting, rollback).

## Acceptance Criteria

- End-to-end happy path verified: device speaks -> AI responds -> audio returns.
- External dependency failures degrade gracefully without crashing gateway.
- Business events are emitted with traceable device context.

## Risks

- Third-party latency can stall user experience.
- Retry storms can overload downstream systems.

## Exit Gate

Pilot environment passes stability and latency targets.
