package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.springframework.stereotype.Component;

@Component
public class NettyUdpInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final UdpPacketProcessor packetProcessor;

    public NettyUdpInboundHandler(UdpPacketProcessor packetProcessor) {
        this.packetProcessor = packetProcessor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        byte[] bytes = new byte[msg.content().readableBytes()];
        msg.content().getBytes(msg.content().readerIndex(), bytes);
        packetProcessor.process(bytes, msg.sender());
    }
}
