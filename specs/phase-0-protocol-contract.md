# Phase 0 - Protocol Contract Freeze

## Goal

Freeze the device-facing contract before coding to avoid compatibility regressions.

## In Scope

- MQTT identity and authentication contract.
- Hello/Goodbye message semantics.
- UDP packet header, encryption, and sequence handling rules.
- Session lifecycle and duplicate connection resolution policy.

## Out of Scope

- ASR/TTS/TRTC integration logic.
- Distributed session storage.

## Contract Items (Must Preserve)

1. `clientId` format: `group@@@mac@@@uuid`.
2. Credentials validation: HMAC-based signature compatibility.
3. `hello` response contains UDP endpoint + `aes-128-ctr` material (`key`, `nonce`).
4. UDP packet format: 16-byte header + payload.
5. Sequence policy: tolerate out-of-order windows, reject replay/invalid packets.
6. `goodbye` and disconnect cleanup semantics.

## Deliverables

- Protocol contract document with JSON examples and field definitions.
- Test vectors for HMAC and AES-CTR compatibility.
- Session state diagram (connected/requesting/streaming/closing).
- Execution taskboard with phase start checklist.

## Delivered Artifacts

- `specs/protocol-contract-v1.md`
- `specs/protocol-test-vectors.md`
- `specs/phase-0-execution.md`

## Acceptance Criteria

- Every field used by firmware and upstream gateway is documented.
- At least 3 positive and 3 negative protocol test vectors exist.
- Team agrees on "new connection replaces old connection" behavior.

## Risks

- Missing one protocol detail causes firmware incompatibility.
- AES-CTR nonce/counter interpretation mismatch causes audio corruption.

## Exit Gate

No implementation starts until this phase is signed off.
