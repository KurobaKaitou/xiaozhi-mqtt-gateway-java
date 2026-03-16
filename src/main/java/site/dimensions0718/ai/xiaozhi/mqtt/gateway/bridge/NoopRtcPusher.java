package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.UdpAudioFrame;

public class NoopRtcPusher implements IRtcPusher {

    @Override
    public void push(UdpAudioFrame frame) {
    }
}
