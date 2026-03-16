# Protocol Compatibility Test Vectors

These vectors are deterministic fixtures for implementation and unit tests.

## 1) HMAC Signature Vectors

### 1.1 Positive vector

- secret (`MQTT_SIGNATURE_KEY`): `AaBbCc123!`
- clientId: `lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932`
- username (plain JSON): `{"ip":"192.168.1.77"}`
- username (base64): `eyJpcCI6IjE5Mi4xNjguMS43NyJ9`
- canonical string:

```text
lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932|eyJpcCI6IjE5Mi4xNjguMS43NyJ9
```

- expected HMAC-SHA256 Base64:

```text
Ur2KvFER5uJK3BF4XzgEi75ckLE9liLRN6eGEJK1g7U=
```

### 1.2 Negative vectors

Reject all cases below:

1. Same inputs but password `Ur2KvFER5uJK3BF4XzgEi75ckLE9liLRN6eGEJK1g7V=` (1 byte changed)
2. Same `clientId` but username changed to `eyJpcCI6IjE5Mi4xNjguMS43OCJ9`
3. Invalid `clientId` format (no `@@@` triple form)

## 2) AES-128-CTR Vectors

### 2.1 Positive vector

- algorithm: `aes-128-ctr`
- key hex: `00112233445566778899aabbccddeeff`
- iv/nonce hex: `0102030405060708090a0b0c0d0e0f10`
- plaintext hex: `48656c6c6f2d7869616f7a68692d6f7075732d6672616d65`
- expected ciphertext hex: `f70045d202582bc286db9a20af765e455c07b93f8e05b7a4`

### 2.2 Round-trip requirement

Decrypting `f70045d202582bc286db9a20af765e455c07b93f8e05b7a4` with same key/iv must produce:

```text
48656c6c6f2d7869616f7a68692d6f7075732d6672616d65
```

## 3) UDP Header Parsing Vectors

Use big-endian decoding.

### 3.1 Positive header sample

- type: `0x01`
- flags: `0x00`
- payload_len: `0x0010` (16)
- connection_id: `0x11223344`
- timestamp: `0x0000003c` (60)
- sequence: `0x0000000a` (10)

Expected header bytes:

```text
01 00 00 10 11 22 33 44 00 00 00 3c 00 00 00 0a
```

### 3.2 Negative header samples

1. total bytes < 16 -> drop
2. payload_len exceeds packet actual size -> drop
3. type != `0x01` -> ignore/drop

## 4) Session Semantics Vectors

1. Same identity reconnects -> old session closed, new session becomes active.
2. `goodbye` for active session -> session removed from index maps.
3. Unknown UDP connection_id -> drop packet, keep server healthy.
