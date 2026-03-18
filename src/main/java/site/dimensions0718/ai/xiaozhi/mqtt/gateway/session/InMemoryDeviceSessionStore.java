package site.dimensions0718.ai.xiaozhi.mqtt.gateway.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryDeviceSessionStore implements IDeviceSessionStore {

    private final ConcurrentHashMap<String, DeviceSession> byClientId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> clientIdByConnectionId = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<DeviceSession> upsert(DeviceSession session) {
        String clientId = session.clientId();

        DeviceSession oldByClient = byClientId.put(clientId, session);
        String oldClientIdForConnection = clientIdByConnectionId.put(session.connectionId(), clientId);

        if (oldByClient != null) {
            clientIdByConnectionId.remove(oldByClient.connectionId());
        }

        if (oldClientIdForConnection != null && !oldClientIdForConnection.equals(clientId)) {
            byClientId.remove(oldClientIdForConnection);
        }

        return Optional.ofNullable(oldByClient);
    }

    @Override
    public Optional<DeviceSession> findByClientId(String clientId) {
        return Optional.ofNullable(byClientId.get(clientId));
    }

    @Override
    public Optional<DeviceSession> findByConnectionId(long connectionId) {
        String clientId = clientIdByConnectionId.get(connectionId);
        if (clientId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byClientId.get(clientId));
    }

    @Override
    public synchronized void removeByClientId(String clientId) {
        DeviceSession removed = byClientId.remove(clientId);
        if (removed == null) {
            return;
        }
        clientIdByConnectionId.remove(removed.connectionId());
    }

    @Override
    public Map<String, DeviceSession> snapshot() {
        return Map.copyOf(byClientId);
    }
}
