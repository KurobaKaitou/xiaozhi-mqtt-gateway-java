# Phase 0 - Execution Taskboard

## Objective

Convert protocol understanding into implementation-ready, testable constraints before writing gateway business code.

## Open-Source Quality Gates

- Clear protocol docs with examples and edge cases.
- Reproducible test vectors committed in repo.
- Security-sensitive behavior explicitly documented (auth, crypto, replay protection).
- No ambiguous field semantics (units, encoding, byte order, required/optional).
- Acceptance criteria are executable as tests, not prose only.

## Work Items

### P0-1 Contract freeze

- Output: `specs/protocol-contract-v1.md`
- Done when:
  - MQTT control message contract complete.
  - UDP binary frame contract complete.
  - Session lifecycle and duplicate policy complete.

### P0-2 Compatibility vectors

- Output: `specs/protocol-test-vectors.md`
- Done when:
  - HMAC signature vectors documented.
  - AES-128-CTR vectors documented.
  - Includes positive and negative examples.

### P0-3 Implementation checklist

- Output: this file section "Phase 1 start checklist"
- Done when:
  - Interfaces and modules for Phase 1 are derivable without guessing.

## Phase 1 Start Checklist

- [ ] `clientId` parsing rules finalized (`group@@@mac@@@uuid`).
- [ ] Auth signature algorithm and canonical input string finalized.
- [ ] Hello/Goodbye payloads and required fields finalized.
- [ ] UDP header field layout and endianness finalized.
- [ ] Sequence anomaly and replay handling finalized.
- [ ] Duplicate connection replacement policy finalized.
- [ ] Device session state machine finalized.
