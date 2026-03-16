package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

public interface IUdpAudioFrameSink {

    void onFrame(UdpAudioFrame frame);
}
