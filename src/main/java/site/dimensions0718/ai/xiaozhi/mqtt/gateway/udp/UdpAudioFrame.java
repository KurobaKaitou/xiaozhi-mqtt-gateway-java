package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.UdpPacketHeader;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;

import java.net.InetSocketAddress;
import java.util.Arrays;

public record UdpAudioFrame(
        DeviceSession session,
        UdpPacketHeader header,
        byte[] decryptedPayload,
        InetSocketAddress remoteAddress
) {

    public UdpAudioFrame {
        decryptedPayload = Arrays.copyOf(decryptedPayload, decryptedPayload.length);
    }

    @Override
    public byte[] decryptedPayload() {
        return Arrays.copyOf(decryptedPayload, decryptedPayload.length);
    }
}
