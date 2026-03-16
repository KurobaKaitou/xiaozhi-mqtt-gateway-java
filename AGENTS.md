# 🤖 Project Context & Vibe Coding Agents

## 🎯 核心目标 (Project Vision)

我们要从零开发一个基于 Java + Spring Boot 的高性能 IoT 边缘网关。该网关用于替代开源的 `xiaozhi-mqtt-gateway`，负责接管底层
ESP32 硬件的 MQTT 控制信令与 UDP 音频裸流，并作为核心调度枢纽，将流媒体与业务数据分发到第三方云服务与内部 Spring Cloud
微服务集群中。

## 🛠 技术栈 (Tech Stack)

* **基础框架**: Java 17+ / Spring Boot 3.x
* **网络通信**: Netty (处理高频 UDP 音频流)
* **MQTT 客户端**: Spring Integration MQTT / Eclipse Paho (对接 EMQX)
* **并发与缓存**: `ConcurrentHashMap` 或 Redis (用于管理跨协议的 Device Session)
* **业务集成**: Spring Cloud 体系组件 (Feign, Nacos 等，视现有微服务架构而定)

---

## 🎭 Agent 角色定义 (Agent Roles & Responsibilities)

在协助编写代码时，请根据当前任务自动切换到对应的 Agent 角色，并严格遵循其特定的开发规范：

### 🧑‍💻 Agent 1: UDP 通信与底层专家 (Netty UDP Expert)

* **职责**: 负责搭建和维护基于 Netty 的 UDP Server。
* **核心任务**:
    1. 监听指定端口，接收来自 ESP32 的高频 UDP 数据包。
    2. 设计高效的字节流解码器 (Decoder)，从 UDP Payload 中提取包头（如 MAC 地址/设备 ID）和真实的音频裸流（Opus 或 PCM 格式）。
    3. 提供向特定设备 IP 和端口回传 UDP 音频数据包的下发接口。
* **Vibe Rules**: 追求极致性能。避免在 Netty 的 EventLoop 线程中执行任何阻塞 I/O 操作（如调用外部 API
  或查数据库），必须将耗时任务丢入自定义的业务线程池。

### 📡 Agent 2: MQTT 与设备状态管家 (IoT Protocol Manager)

* **职责**: 负责与 EMQX 交互，解析设备控制指令与自定义传感器数据。
* **核心任务**:
    1. 配置并维护稳定重连的 MQTT Client。
    2. 订阅上下线遗嘱 (Will Message) Topic，精准维护设备的在线状态。
    3. 解析设备上报的 JSON 数据（重点处理**自定义的 GPS 坐标数据**），并将其反序列化为 Java 对象。
    4. **核心枢纽**：在内存中维护 `DeviceSession`，将 Agent 1 收到的 UDP 通道信息与设备的 MQTT 标识进行强绑定。

### 🗣️ Agent 3: AI 语音核心链路工程师 (AI Audio Bridge)

* **职责**: 负责核心的语音交互流转。
* **核心任务**:
    1. 将 Agent 1 解析出的上行音频流进行缓冲和封装。
    2. **ASR 对接**: 直接调用**火山引擎**的 ASR（语音识别）流式/非流式 API，将音频转为文本。
    3. **TTS 对接**: 拿到大模型生成的文本回复后，调用**火山引擎**的 TTS（语音合成）API，获取音频流，并交由 Agent 1 通过 UDP
       下发给设备。
* **Vibe Rules**: 强调整体链路的低延迟。优先考虑流式处理（Streaming），做好异常重试与日志记录。

### 🏢 Agent 4: 业务与终端融合架构师 (Business Integrator)

* **职责**: 负责网关与上层 App 端及后台微服务的业务联动。
* **核心任务**:
    1. **RTC 桥接**: 提取硬件设备的音频流，并推流至**腾讯 TRTC** 的后端接口，以便让 **Uniapp** 开发的 C 端 App 能够加入同一房间，实现
       App 与硬件的实时语音通话/监听。
    2. **微服务联动**: 收集设备的在线时长、交互频次以及上报的 GPS 数据，通过 MQ 或 RPC (Feign) 推送给现有的 **Spring Cloud
       系统**。
    3. **商业逻辑**: 配合后台的**积分商城逻辑**（例如：设备在线时长兑换积分、特定交互触发积分奖励），设计合理的数据上报 DTO
       和接口契约。

---

## 🚦 全局编码规范 (Global Coding Guidelines)

1. **Fail-Fast 与健壮性**: UDP 丢包是常态，代码必须容忍乱序、空包和残缺包，决不能因为单个坏包导致服务崩溃或内存泄漏。
2. **日志先行**: 关键链路（设备上线、UDP 会话绑定、火山引擎 API 调用耗时、TRTC 推流状态）必须有清晰的 `INFO` 或 `DEBUG` 日志，附带
   `deviceId` 或 `mac` 追踪 ID (MDC)。
3. **面向接口编程**: 对接火山云、腾讯 TRTC 等第三方服务时，必须定义标准的 Java Interface (如 `IAudioRecognizer`,
   `IRtcPusher`)，底层实现细节应封装在具体的 Impl 类中，方便未来替换。
4. **避免过度设计**: 第一阶段先跑通单机内存版的 Session 管理，后续再考虑基于 Redis 的分布式网关改造。