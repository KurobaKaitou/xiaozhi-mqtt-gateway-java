# AGENTS Guide: xiaozhi-mqtt-gateway-java

This file is for autonomous coding agents operating in this repository.
It is based on current code and config, not generic Java guidance.

## 1) Project Snapshot

- Purpose: Java/Spring Boot gateway for xiaozhi device MQTT control + UDP audio.
- Core flow: MQTT handshake/control, UDP audio up/downlink, WebSocket bridge.
- Build system: Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Java version: 21 (`pom.xml`).
- Spring Boot: `3.5.11` parent in `pom.xml`.

Primary dependencies visible in `pom.xml`:
- `spring-boot-starter-actuator`
- `spring-boot-starter-integration`
- `spring-boot-starter-webflux`
- `spring-integration-mqtt`
- `org.eclipse.paho.client.mqttv3`
- `fastjson2`
- `lombok`
- test deps: `spring-boot-starter-test`, `reactor-test`, `spring-integration-test`

## 2) Mandatory Command Set

Always prefer Maven Wrapper over system Maven.

```bash
# compile
./mvnw clean compile

# run app
./mvnw spring-boot:run

# full tests (documented in README)
./mvnw clean test

# single test class (`-Dtest` selector; no explicit Surefire plugin declared in `pom.xml`)
./mvnw -Dtest=ClassNameTests test

# single test method (`-Dtest` selector)
./mvnw -Dtest=ClassNameTests#methodName test

# multiple test classes
./mvnw "-Dtest=ClassATests,ClassBTests" test

# integration-test selector for future use only; no Failsafe plugin is currently declared
./mvnw -Dit.test=MyIT verify
```

Repo-specific examples:

```bash
./mvnw -Dtest=MqttDeviceMessageHandlersTests test
./mvnw -Dtest=MqttDeviceMessageHandlersTests#mcpReturnsGoodbyeWhenBridgeUnavailable test
```

Quality gate for most changes:

```bash
./mvnw clean compile
./mvnw test
```

Build plugin status in this repository:
- `pom.xml` declares `maven-compiler-plugin` and `spring-boot-maven-plugin`.
- No dedicated lint/format/coverage plugins (for example Checkstyle, Spotless, PMD, or JaCoCo) are declared.
- No explicit `maven-surefire-plugin` or `maven-failsafe-plugin` declaration was found; standard `test` execution still works through Maven's default lifecycle.
- Use compile + tests as baseline verification.

## 3) Test Structure and Conventions

- Test source root: `src/test/java`.
- Framework: JUnit 5 (`org.junit.jupiter.api.Test`).
- Common naming: `*Tests` (plural) appears frequently.
- Test packages mirror production package paths.
- Favor descriptive lowerCamelCase method names describing behavior.

## 4) Coding Style Rules (Observed in Codebase)

### Imports

- Prefer explicit imports in production code.
- Static imports are common in tests where readability improves.
- Wildcard imports exist in a few production files; prefer explicit imports in new code.

### Formatting

- Use 4-space indentation.
- Keep opening brace on same line.
- Keep methods focused.
- Wrap long constructors one parameter per line.
- Separate logical blocks with a blank line.

### Dependency Injection and Types

- Prefer constructor injection with `private final` fields.
- Keep integration boundaries interface-first.
- Existing naming convention includes interface prefix `I` (for example `IAudioRecognizer`).
- Implementations may use `*Impl` where interface pairing exists.
- Use `record` for simple immutable value/transport objects when appropriate.

### Naming and Packaging

- Base package: `site.dimensions0718.ai.xiaozhi.mqtt.gateway`.
- Feature-oriented packages are dominant (`bridge`, `udp`, `handler`, `service`, `session`, `protocol`, `web`, `config`).
- Common suffixes: `*Handler`, `*Service`, `*Controller`, `*Config`, `*Properties`.
- Prefer intention-revealing names; avoid cryptic abbreviations.

### Error Handling and Resilience

- Fail fast at boundaries for invalid critical input.
- In runtime pipelines (MQTT/UDP), catch/log/drop malformed messages safely.
- Never use empty catch blocks.
- Never suppress exceptions silently.

### Logging

- Use SLF4J placeholder style logging.
- Include useful correlation identifiers (`deviceId`, `mac`, topic, clientId) on key events.
- Both `LoggerFactory` and Lombok `@Slf4j` exist; follow local file style when editing.

### Concurrency and Networking

- Assume UDP packet loss/reorder/truncation is normal.
- Keep packet-handling paths robust to malformed frames.
- Avoid blocking I/O on Netty event loop related paths.
- Keep session state transitions explicit and thread-safe.

## 5) Change Discipline

- Prefer minimal, surgical diffs.
- Match existing package and naming patterns before introducing new abstractions.
- Hide external-provider details behind interfaces.
- Add comments only for non-obvious protocol/crypto/concurrency logic.
- Do not introduce unrelated refactors in functional change PRs.

## 6) Cursor/Copilot Rules Check

Checked locations:
- `.cursor/rules/`
- `.cursorrules`
- `.github/copilot-instructions.md`

Current result in this repository:
- No repo-local Cursor rules were found at the checked paths.
- No repo-local Copilot instruction file was found at the checked path.

If these files are added later, update this AGENTS guide and treat conflicts as high-priority review items.

## 7) Completion Checklist for Agents

For non-trivial code changes, finish only when all are true:

1. Code compiles (`./mvnw clean compile`).
2. Related tests pass (single-class/method first when practical).
3. Wider tests pass when multiple modules are touched.
4. Logging/error paths are validated for changed runtime flow.
5. Naming/package/style remain consistent with rules above.

Keep this file current when build plugins, test strategy, or style conventions change.
