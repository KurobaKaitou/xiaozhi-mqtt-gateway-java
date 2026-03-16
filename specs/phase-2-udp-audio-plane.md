# Phase 2 - UDP Audio Plane (Netty)

## Goal

Deliver a robust Netty UDP pipeline that decodes, validates, decrypts, and routes device audio packets safely.

## In Scope

- Netty UDP server bootstrap.
- 16-byte header parsing and payload bounds checks.
- AES-128-CTR decrypt flow.
- Sequence/timestamp validation and replay protection.
- Backpressure and drop strategy for burst traffic.

## Out of Scope

- Full ASR/TTS business pipeline integration.
- Multi-region distributed packet routing.

## Work Breakdown

1. Build UDP channel pipeline and decoder.
2. Bind UDP packets to existing `DeviceSession` using connection/session key.
3. Implement strict packet validation:
   - short packet drop
   - payload length mismatch drop
   - unknown session drop
4. Implement decrypt and sequencing policy.
5. Offload any blocking or heavy downstream work from EventLoop.
6. Add packet metrics:
   - total packets
   - decrypt failures
   - sequence anomalies
   - dropped packets by reason.

## Deliverables

- UDP module with deterministic decode/decrypt behavior.
- Integration tests with valid and malformed packet fixtures.
- Performance baseline report under synthetic packet load.

## Acceptance Criteria

- Valid packet path reaches audio sink/bridge without blocking EventLoop.
- Malformed packets are dropped and logged, service remains stable.
- Sequence anomaly handling matches agreed protocol contract.
- No unbounded queue growth under stress test.

## Risks

- CTR counter mismatch can silently corrupt audio stream.
- Overly strict sequence policy may drop legitimate late packets.

## Exit Gate

UDP path passes compatibility fixtures and load sanity tests.
