# xiaozhi-mqtt-gateway-java

基于 **Java 21 + Spring Boot 3 + Netty + Spring Integration MQTT** 的 IoT 边缘网关，目标替代上游 `xinnan-tech/xiaozhi-mqtt-gateway`，承接 ESP32 设备的 MQTT 控制信令和 UDP 音频通道。

## 当前状态

- 已实现：协议基础能力（clientId/HMAC/AES-CTR/UDP 头）、MQTT 控制面、UDP 入站处理链、管理 API、替代就绪检查。
- 已实现：EMQX 接入适配（可开关）、静态/动态 topic 双模式解析。
- 未完成：WebSocket 桥接到聊天服务、真实 ASR/TTS/TRTC 适配器、分布式 Session（Redis）、完整端到端生产联调。

> 结论：当前版本是“可联调的阶段性实现”，**不是**上游项目的完全等价替代。

## 非目标与限制

- 不是上游 Node 网关的 1:1 功能复刻（尤其是 `WebSocketBridge` + MCP 核心链路）。
- 当前会话存储为内存版，重启会丢失；不适合多实例直接横向扩展。
- 当前桥接适配器默认 Noop，仅定义了接口边界，未接真实云厂商实现。
- 管理 API token 属于轻量内部鉴权（按日 token），不是零信任级别认证方案。

## 功能矩阵（相对上游）

| 能力 | 当前 Java 项目 | 上游 Node 项目 |
|---|---|---|
| MQTT 接入模型 | EMQX + Spring Integration MQTT 客户端 | 自建/手写 MQTT 协议处理 |
| UDP 音频入站解析 | 已实现（Netty + 头校验 + 解密） | 已实现 |
| 会话绑定（MQTT/UDP） | 已实现（内存版） | 已实现 |
| WebSocketBridge 核心链路 | 未实现（仅接口骨架） | 已实现（核心路径） |
| MCP 请求透传闭环 | 部分（管理 API 入口有，设备回执闭环待补） | 已实现 |
| 管理 API 与鉴权 | 已实现 | 已实现 |
| ASR/TTS/TRTC 真实对接 | 未实现（Noop） | 由上游生态链路承接 |
| 分布式会话（Redis） | 未实现 | 未实现（当前上游亦偏单机） |

## 与上游项目差异（重点）

### 已对齐/等价能力

- 设备身份格式和签名校验基础：`group@@@mac@@@uuid` + `HMAC(clientId|username)`。
- `hello/goodbye` 语义与会话生命周期基础。
- UDP 16 字节头解析、AES-128-CTR 解密、基础序列窗口防重放。
- 管理 API 鉴权规则（`SHA256(yyyy-MM-dd + MQTT_SIGNATURE_KEY)`）与接口形态：
  - `POST /api/commands/{clientId}`
  - `POST /api/devices/status`

### 架构性差异

- 上游（Node）内置自定义 MQTT Server（TCP 直连协议解析）。
- 当前（Java）采用 **EMQX Broker + Spring Integration MQTT 客户端模式**。
- 上游强依赖 WebSocketBridge（设备音频/事件桥接聊天服务）；当前仅提供桥接接口骨架，默认 Noop 实现。

### 兼容性前提（必须满足）

- 若使用静态 topic `device-server`，payload 必须包含 `client_id`（或你配置的字段名）。
- 设备协议需符合当前约定：`clientId=group@@@mac@@@uuid`、`hello version=3`、`transport=udp`。
- AES-CTR 的 key/nonce/报文格式需与当前实现一致；如设备固件有变体，需先做兼容验证。

### 当前缺口（替代前必须补）

- WebSocketBridge 等价链路（上游 `app.js` 核心逻辑）未落地。
- 真实 ASR/TTS/TRTC 适配器未接入（目前是接口 + 默认空实现）。
- 生产级观测、限流、重试/熔断、压测基线还需补齐。

## 架构概览

```text
Device (MQTT/UDP)
  -> EMQX
    -> Spring Integration MQTT Inbound
      -> MqttInboundMessageDispatcher
        -> MqttControlService (hello/goodbye/session)
          -> SessionStore (in-memory)
            -> UDP key/nonce/session binding

Device UDP Audio
  -> Netty UDP Server
    -> UdpPacketProcessor (header check + decrypt + sequence window)
      -> IUdpAudioFrameSink (AudioBridgeUdpFrameSink)
        -> IAudioRecognizer / IAudioSynthesizer / IRtcPusher / IBusinessEventPublisher
```

## 快速开始

### 1) 环境要求

- Java 21+
- Maven 3.8+
- EMQX（你当前可用 `114.132.160.185:1883`）

### 2) 关键环境变量

```bash
# 必需
MQTT_SIGNATURE_KEY=YourStrongKey123

# 建议（本地联调）
GATEWAY_MQTT_ENABLED=true
GATEWAY_UDP_ENABLED=true
GATEWAY_MQTT_SERVER_URI=tcp://114.132.160.185:1883
PUBLIC_IP=127.0.0.1
UDP_PORT=8884
```

### 3) 启动

```bash
./mvnw spring-boot:run
```

### 4) 运行测试

```bash
./mvnw test
```

## 配置项说明（`application.yaml`）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `gateway.mqtt.enabled` | `${GATEWAY_MQTT_ENABLED:true}` | 是否启用 MQTT 适配层 |
| `gateway.mqtt.server-uri` | `${GATEWAY_MQTT_SERVER_URI:tcp://114.132.160.185:1883}` | EMQX 地址 |
| `gateway.mqtt.client-id-prefix` | `xiaozhi-java-gateway` | 网关 MQTT clientId 前缀 |
| `gateway.mqtt.username` | 空 | EMQX 用户名 |
| `gateway.mqtt.password` | 空 | EMQX 密码 |
| `gateway.mqtt.inbound-topic` | `device-server` | 上行订阅 topic |
| `gateway.mqtt.outbound-topic-template` | `device/{clientId}/down` | 下行发送 topic 模板 |
| `gateway.mqtt.client-id-payload-field` | `client_id` | 静态 topic 模式下 payload 中 clientId 字段 |
| `gateway.mqtt.qos` | `0` | MQTT QoS |
| `gateway.runtime.udp-public-host` | `${PUBLIC_IP:mqtt.xiaozhi.me}` | 回给设备的 UDP 服务器地址 |
| `gateway.runtime.udp-port` | `${UDP_PORT:8884}` | 回给设备的 UDP 端口 |
| `gateway.udp.enabled` | `${GATEWAY_UDP_ENABLED:true}` | 是否启动 Netty UDP 服务 |
| `gateway.udp.bind-host` | `0.0.0.0` | UDP 绑定地址 |
| `gateway.udp.bind-port` | `${UDP_PORT:8884}` | UDP 监听端口 |
| `gateway.udp.worker-threads` | `0` | UDP worker 线程数，0=按 CPU 自动 |
| `gateway.security.signature-key` | `${MQTT_SIGNATURE_KEY:}` | 设备签名和管理 API token 的密钥 |

## MQTT Topic 使用说明

当前支持两种上行模式：

1. **静态 Topic（默认，参考上游）**
   - 上行：`device-server`
   - 要求：payload 内必须包含 `client_id`（或配置字段 `gateway.mqtt.client-id-payload-field`）

2. **动态 Topic**
   - 上行：`device/{clientId}/up`
   - 无需从 payload 提取 clientId

下行统一通过模板：`device/{clientId}/down`

## 管理 API

所有 `/api/**` 接口都需要 `Authorization: Bearer <dailyToken>`。

- token 规则：`SHA256(yyyy-MM-dd + MQTT_SIGNATURE_KEY)`（十六进制小写）
- 安全说明：这是轻量运维鉴权，存在日期窗口内重放风险，生产建议叠加网关白名单、反向代理限流与更强鉴权机制。

### 1) 设备指令下发

```bash
curl -X POST "http://localhost:8080/api/commands/<clientId>" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <dailyToken>" \
  -d '{"type":"mcp","payload":{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"self.get_device_status","arguments":{}}}}'
```

### 2) 设备状态查询

```bash
curl -X POST "http://localhost:8080/api/devices/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <dailyToken>" \
  -d '{"clientIds":["<clientId>"]}'
```

### 3) 替代就绪检查

```bash
curl -X POST "http://localhost:8080/api/system/replacement-readiness" \
  -H "Authorization: Bearer <dailyToken>"
```

## 目录结构（核心）

```text
src/main/java/.../protocol   协议工具（身份、签名、AES、UDP头）
src/main/java/.../session    会话模型与内存存储
src/main/java/.../service    控制面、分发器、鉴权、就绪检查
src/main/java/.../udp        Netty UDP 生命周期与处理链
src/main/java/.../bridge     音频桥接接口与默认适配器
src/main/java/.../web        管理 API 与认证过滤器
specs/                       分阶段规格文档
```

## Roadmap

- [ ] 补齐 WebSocketBridge 等价能力（对齐上游 app.js 关键路径）
- [ ] 接入火山引擎 ASR/TTS 真实实现
- [ ] 接入腾讯 TRTC 推流实现
- [ ] Redis 化 DeviceSession（多实例网关）
- [ ] 增加压测报告、指标与告警模板

## 开源协作

- Issue: 提交 bug/需求（请附最小复现）
- PR: 建议拆小提交，附测试说明
- 不提交敏感配置（密钥、生产地址凭据）

## License

待补充（建议 MIT 或 Apache-2.0）。
