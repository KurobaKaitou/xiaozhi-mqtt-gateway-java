# EMQX 接入方案（第一种：Topic 携带 clientId）

目标：设备仍然发布到原始 topic（如 `device-server`）时，通过 EMQX 规则重写为 `device/{clientId}/up`，让 Java 网关从 topic 直接提取 `clientId`。

## 1. 网关侧配置

确保网关订阅动态 topic：

```yaml
gateway:
  mqtt:
    enabled: true
    inbound-topic: device/+/up
    outbound-topic-template: device/{clientId}/down
```

## 2. EMQX Rule Engine

在 EMQX Dashboard -> Rule Engine 中新增规则：

### SQL

```sql
SELECT
  clientid,
  payload,
  topic,
  qos,
  timestamp
FROM "device-server"
```

### Action: Republish

- Target Topic: `device/${clientid}/up`
- Payload Template: `${payload}`
- QoS: `${qos}`

说明：

- `clientid` 来自设备 MQTT CONNECT 阶段（由 EMQX感知）。
- Java 网关无需再依赖 payload 中的 `client_id` 字段。

## 3. 下行路径

网关默认下行 topic：

```text
device/{clientId}/down
```

设备需订阅其对应的 down topic。

## 4. 验证步骤

1. 启动网关（`GATEWAY_MQTT_ENABLED=true`）。
2. 设备发布 `hello` 到 `device-server`（payload 不必带 `client_id`）。
3. EMQX 规则命中并转发到 `device/{clientId}/up`。
4. 网关日志出现 `hello` 处理成功，并返回 down topic 响应。

## 5. 常见问题

- 规则命中但网关无响应：检查 `GATEWAY_MQTT_INBOUND_TOPIC` 是否是 `device/+/up`。
- 设备收不到下行：确认设备订阅的是 `device/{clientId}/down`。
- 多设备混发：确保每个设备 CONNECT 的 `clientid` 唯一。
