package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class UdpMetrics {

    private final AtomicLong acceptedFrames = new AtomicLong();
    private final EnumMap<UdpDropReason, AtomicLong> droppedByReason = new EnumMap<>(UdpDropReason.class);

    public UdpMetrics() {
        for (UdpDropReason reason : UdpDropReason.values()) {
            droppedByReason.put(reason, new AtomicLong());
        }
    }

    public void markAccepted() {
        acceptedFrames.incrementAndGet();
    }

    public void markDropped(UdpDropReason reason) {
        droppedByReason.get(reason).incrementAndGet();
    }

    public long acceptedFrames() {
        return acceptedFrames.get();
    }

    public Map<UdpDropReason, Long> droppedSnapshot() {
        EnumMap<UdpDropReason, Long> snapshot = new EnumMap<>(UdpDropReason.class);
        for (Map.Entry<UdpDropReason, AtomicLong> entry : droppedByReason.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return Map.copyOf(snapshot);
    }
}
