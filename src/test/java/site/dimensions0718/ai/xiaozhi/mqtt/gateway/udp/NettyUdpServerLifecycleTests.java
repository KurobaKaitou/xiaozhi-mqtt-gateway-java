package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.UdpServerProperties;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NettyUdpServerLifecycleTests {

    @Test
    void shouldNotStartWhenDisabled() {
        UdpServerProperties properties = new UdpServerProperties();
        properties.setEnabled(false);

        UdpPacketProcessor processor = Mockito.mock(UdpPacketProcessor.class);
        NettyUdpInboundHandler handler = new NettyUdpInboundHandler(processor);
        NettyUdpServerLifecycle lifecycle = new NettyUdpServerLifecycle(properties, handler);

        lifecycle.start();
        assertFalse(lifecycle.isRunning());

        lifecycle.stop();
        assertFalse(lifecycle.isRunning());
    }
}
