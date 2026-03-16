# Protocol Contract v1 (Device-Facing)

## Scope

This contract defines behavior that must stay compatible with upstream gateway behavior for ESP32 devices.

## 1) Identity and Authentication

### 1.1 clientId format

- Format: `group@@@mac@@@uuid`
- Example: `lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932`
- Rules:
  - exactly 3 parts separated by `@@@`
  - `mac` uses `_` separator in MQTT `clientId` payload, normalized to `:` in server memory

### 1.2 username format

- Base64-encoded JSON string.
- Typical JSON includes client IP metadata.

### 1.3 password signature

- HMAC-SHA256 over canonical string: `clientId + "|" + username`
- Secret: `MQTT_SIGNATURE_KEY`
- Digest encoding: Base64

## 2) MQTT Control Channel Contract

### 2.1 hello request (device -> gateway)

```json
{
  "type": "hello",
  "version": 3,
  "transport": "udp",
  "features": {
    "mcp": true
  },
  "audio_params": {
    "format": "opus",
    "sample_rate": 16000,
    "channels": 1,
    "frame_duration": 60
  }
}
```

### 2.2 hello response (gateway -> device)

```json
{
  "type": "hello",
  "version": 3,
  "session_id": "<session-id>",
  "transport": "udp",
  "udp": {
    "server": "<public-ip-or-domain>",
    "port": 8884,
    "encryption": "aes-128-ctr",
    "key": "<hex-encoded-16-byte-key>",
    "nonce": "<hex-encoded-16-byte-nonce>"
  },
  "audio_params": {
    "format": "opus",
    "sample_rate": 24000,
    "channels": 1,
    "frame_duration": 60
  }
}
```

### 2.3 goodbye handling

- Device `goodbye` closes bridge/session.
- Gateway may actively emit `goodbye` on bridge/session shutdown.

### 2.4 qos assumptions

- Control path behavior is QoS 0 compatible in upstream behavior.
- Messages should be idempotent where needed.

## 3) UDP Audio Channel Contract

## 3.1 packet frame

- Total frame: `header(16 bytes) + payload(payload_len bytes)`
- Byte order: network byte order (big-endian)

| Offset | Size | Field |
|---|---:|---|
| 0 | 1 | type (must be `0x01`) |
| 1 | 1 | flags (reserved) |
| 2 | 2 | payload_len |
| 4 | 4 | connection_id / ssrc |
| 8 | 4 | timestamp |
| 12 | 4 | sequence |

### 3.2 decrypt

- Algorithm: AES-128-CTR
- key: from `hello.udp.key`
- nonce/iv: from `hello.udp.nonce` or header-derived counter semantics based on compatibility vectors

### 3.3 sequence policy

- Reject clearly stale/replay packets.
- Log anomalies for jumps/out-of-order and continue based on configured tolerance.
- Bad packet must never crash pipeline.

## 4) Session Lifecycle Contract

States:

1. `DISCONNECTED`
2. `MQTT_CONNECTED`
3. `CHANNEL_REQUESTED` (after `hello` request)
4. `UDP_ACTIVE`
5. `CLOSING`

Rules:

- New valid connection for same identity replaces old active connection.
- Session bind is atomic: auth + hello + crypto material + routing key.
- Timeout and disconnect must clean session and indexes deterministically.

## 5) Management API Compatibility Surface

- `POST /api/commands/{clientId}` for command relay.
- `POST /api/devices/status` for online/alive check.
- Auth: bearer token derived from date + signature key.

## 6) Fail-Fast Expectations

- Invalid `clientId` format -> reject.
- Invalid auth signature -> reject.
- Malformed JSON -> reject and log.
- UDP short packet / length mismatch / unknown session -> drop and log.

## 7) Observability Baseline

Every lifecycle event logs with MDC key `deviceId` or `mac`:

- connect/auth pass/fail
- hello accepted/rejected
- session replaced (duplicate connection)
- udp decrypt failure
- timeout/disconnect/goodbye
