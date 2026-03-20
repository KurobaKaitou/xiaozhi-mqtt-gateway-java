# xiaozhi-mqtt-gateway-java

这是一个基于 **Java + Spring Boot + Netty + EMQX** 的网关项目，用来承接 xiaozhi 设备的 MQTT 控制消息和 UDP 音频流。

项目目标：在保留设备协议兼容性的前提下，用 Java 实现可维护、可扩展的网关能力。

注: EMQX 服务需要自行部署在本地/服务器上 您可以采用Docker来运行以下命令来进行部署 EMQX 服务

```bash
docker run -d --name emqx-enterprise \
  --restart=always \
  -p 1883:1883 -p 8083:8083 \
  -p 8084:8084 -p 8883:8883 \
  -p 18083:18083 \
  -v emqx_data:/opt/emqx/data \
  -v emqx_log:/opt/emqx/log \
  -v emqx_etc:/opt/emqx/etc \
  emqx/emqx-enterprise:6.1.1
```

---

## 这个项目是做什么的

简单说，它做三件事：

1. 处理设备 MQTT 控制消息（如 `hello` / `goodbye`）
2. 处理设备 UDP 音频流（解包、解密、下发）
3. 把设备音频桥接到 WebSocket 服务端（用于语音链路）

---

## 核心逻辑（简版）

### 1) 设备上线握手

- 设备通过 MQTT 发 `hello`
- 网关创建 `DeviceSession`
- 网关回 `hello`（包含 UDP 地址、端口、加密参数 key/nonce）

### 2) 音频上行（设备 -> 网关）

- 设备把音频发到网关 UDP 端口
- 网关按协议头解析、做 AES-CTR 解密
- 解密后的音频转发到 WS bridge

### 3) 音频下行（服务端 -> 设备）

- WS 收到下行音频后，网关封包并加密
- 通过 UDP 下发给设备

### 4) 会话结束

- 设备发 `goodbye` 时，网关关闭对应 bridge 并清理会话
- 不强制断开设备与 EMQX 的 TCP 连接（符合当前架构语义）

---

## 配置说明（application.yaml）

配置文件：`src/main/resources/application.yaml`

### `gateway.mqtt`

- `enabled`：是否启用 MQTT 适配层
- `server-uri`：EMQX 地址，例如 `tcp://ip:1883`
- `inbound-topic`：设备上行发布主题（默认 `devices/p2p/+`）
- `outbound-topic-template`：主下行发布主题模板（默认 `device-server`）
- `compatibility-outbound-topics`：兼容下行主题（用于不同固件兼容）

### `gateway.runtime`

- `udp-public-host`：回给设备的 UDP 地址（设备可达）
- `udp-port`：回给设备的 UDP 端口

### `gateway.udp`

- `enabled`：是否启用 UDP 服务
- `bind-host` / `bind-port`：UDP 本地监听地址与端口

### `gateway.bridge`

- `enabled`：是否启用 WS 桥接
- `chat-servers`：WS 服务地址列表
- `server-secret`：WS 鉴权密钥（要与服务端一致）

### `gateway.security`

- `signature-key`：设备签名校验密钥（对应 `MQTT_SIGNATURE_KEY`）

---

## 设备 topic 建议

### 设备上行（publish）

- `devices/p2p/{macRaw}`

### 设备下行（subscribe）

- `device-server`（主通道）

> 如果设备提示 `waiting response timeout`，优先检查：设备是否订阅到了网关实际回包的 topic。

---

## 快速启动

### 1. 环境变量（示例）

```bash
GATEWAY_MQTT_ENABLED=true
GATEWAY_MQTT_SERVER_URI=tcp://<EMQX_IP>:1883
GATEWAY_UDP_ENABLED=true
GATEWAY_BRIDGE_ENABLED=true
GATEWAY_BRIDGE_CHAT_SERVERS=ws://<WS_HOST>/xiaozhi/v1/?from=mqtt_gateway
SERVER_SECRET=<你的WS密钥>
MQTT_SIGNATURE_KEY=<你的签名密钥>
PUBLIC_IP=<设备可达IP>
UDP_PORT=8884
```

### 2. 启动

```bash
./mvnw spring-boot:run
```

### 3. 测试

```bash
./mvnw clean test
```

---

## 常见问题

### 1) 设备显示等待响应超时

- 云服务器 MQTT 相关端口是否开放
- 网关是否收到 `hello`（看日志）
- 网关是否成功发布下行消息
- 设备是否订阅正确 topic
- EMQX ACL 是否放行

### 2) WS 提示认证失败

- 检查 `SERVER_SECRET` 与服务端是否一致
- 检查 WS 地址格式是否正确（`ws://` 不是 `ws:/`）

### 3) 有上行声音但设备无下行声音

- 检查是否有 `websocket binary downlink result ... sent=true`
- 检查 `PUBLIC_IP` 是否设备可达
- 检查 UDP 端口是否放行

---

## 当前进度说明

本项目目前已经可以跑通核心链路联调（MQTT + UDP + WS bridge），但仍在持续完善生产级能力（如分布式会话、完整监控、更多容错策略等）。
