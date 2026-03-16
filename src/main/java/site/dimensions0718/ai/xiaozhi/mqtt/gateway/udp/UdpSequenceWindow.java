package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import java.util.concurrent.ConcurrentHashMap;

public class UdpSequenceWindow {

    private final long tolerance;
    private final ConcurrentHashMap<Long, Long> highestSequenceByConnection = new ConcurrentHashMap<>();

    public UdpSequenceWindow(long tolerance) {
        if (tolerance < 0) {
            throw new IllegalArgumentException("tolerance must be >= 0");
        }
        this.tolerance = tolerance;
    }

    public boolean accept(long connectionId, long sequence) {
        Long previous = highestSequenceByConnection.get(connectionId);
        if (previous == null) {
            highestSequenceByConnection.put(connectionId, sequence);
            return true;
        }

        if (sequence > previous) {
            highestSequenceByConnection.put(connectionId, sequence);
            return true;
        }

        if (sequence == previous) {
            return false;
        }

        return sequence + tolerance >= previous;
    }
}
