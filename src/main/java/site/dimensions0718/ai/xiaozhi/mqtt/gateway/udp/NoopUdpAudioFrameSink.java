package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(IUdpAudioFrameSink.class)
public class NoopUdpAudioFrameSink implements IUdpAudioFrameSink {

    @Override
    public void onFrame(UdpAudioFrame frame) {
        // Intentionally no-op for Phase 2 baseline.
    }
}
