package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp.UdpAudioFrame;

public interface IRtcPusher {

    void push(UdpAudioFrame frame);
}
