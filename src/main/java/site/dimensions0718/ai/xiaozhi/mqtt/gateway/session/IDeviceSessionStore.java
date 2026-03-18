package site.dimensions0718.ai.xiaozhi.mqtt.gateway.session;

import java.util.Map;
import java.util.Optional;

public interface IDeviceSessionStore {

    Optional<DeviceSession> upsert(DeviceSession session);

    Optional<DeviceSession> findByClientId(String clientId);

    Optional<DeviceSession> findByConnectionId(long connectionId);

    void removeByClientId(String clientId);

    Map<String, DeviceSession> snapshot();
}
