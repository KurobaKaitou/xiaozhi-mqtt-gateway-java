package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.AesCtrCodec;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.UdpPacketHeader;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.DeviceSession;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@Service
public class UdpDownlinkService {

    private static final Logger log = LoggerFactory.getLogger(UdpDownlinkService.class);

    private final ObjectProvider<NettyUdpServerLifecycle> udpServerLifecycleProvider;

    public UdpDownlinkService(ObjectProvider<NettyUdpServerLifecycle> udpServerLifecycleProvider) {
        this.udpServerLifecycleProvider = udpServerLifecycleProvider;
    }

    public boolean sendAudio(DeviceSession session, byte[] opusPayload, long timestamp) {
        InetSocketAddress remoteAddress = session.udpRemoteAddress();
        if (remoteAddress == null || opusPayload == null || opusPayload.length == 0) {
            return false;
        }

        long sequence = session.nextUdpLocalSequence();
        byte[] header = encodeHeader(session.connectionId(), timestamp, sequence, opusPayload.length);
        byte[] encrypted = AesCtrCodec.encrypt(opusPayload, session.udpKey(), header);
        byte[] message = Arrays.copyOf(header, header.length + encrypted.length);
        System.arraycopy(encrypted, 0, message, header.length, encrypted.length);

        NettyUdpServerLifecycle lifecycle = udpServerLifecycleProvider.getIfAvailable();
        if (lifecycle != null && lifecycle.sendDatagram(message, remoteAddress)) {
            log.debug("udp downlink sent via netty channel, clientId={}, remote={}", session.clientId(), remoteAddress);
            return true;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(message, message.length, remoteAddress);
            socket.send(packet);
            log.warn("udp downlink used fallback ephemeral socket, clientId={}, remote={}", session.clientId(), remoteAddress);
            return true;
        } catch (Exception ex) {
            log.warn("failed to send udp downlink for clientId={}", session.clientId(), ex);
            return false;
        }
    }

    private static byte[] encodeHeader(long connectionId, long timestamp, long sequence, int payloadLength) {
        ByteBuffer buffer = ByteBuffer.allocate(UdpPacketHeader.HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);
        buffer.putShort((short) payloadLength);
        buffer.putInt((int) connectionId);
        buffer.putInt((int) timestamp);
        buffer.putInt((int) sequence);
        return buffer.array();
    }
}
