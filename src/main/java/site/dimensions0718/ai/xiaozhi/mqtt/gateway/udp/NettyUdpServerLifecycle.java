package site.dimensions0718.ai.xiaozhi.mqtt.gateway.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.UdpServerProperties;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NettyUdpServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(NettyUdpServerLifecycle.class);

    private final UdpServerProperties properties;
    private final NettyUdpInboundHandler inboundHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile EventLoopGroup eventLoopGroup;
    private volatile Channel channel;

    public NettyUdpServerLifecycle(UdpServerProperties properties, NettyUdpInboundHandler inboundHandler) {
        this.properties = properties;
        this.inboundHandler = inboundHandler;
    }

    @Override
    public void start() {
        if (!properties.isEnabled()) {
            log.info("UDP server disabled by config: gateway.udp.enabled=false");
            return;
        }

        if (running.get()) {
            return;
        }

        int threads = properties.getWorkerThreads() <= 0 ? Runtime.getRuntime().availableProcessors() : properties.getWorkerThreads();
        this.eventLoopGroup = new NioEventLoopGroup(threads);

        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .handler(inboundHandler);

        this.channel = bootstrap.bind(properties.getBindHost(), properties.getBindPort()).syncUninterruptibly().channel();
        this.running.set(true);
        log.info("UDP server started on {}:{} with {} worker threads",
                properties.getBindHost(), properties.getBindPort(), threads);
    }

    @Override
    public void stop() {
        if (!running.get()) {
            return;
        }

        try {
            if (channel != null) {
                channel.close().syncUninterruptibly();
            }
        } finally {
            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully().syncUninterruptibly();
            }
            running.set(false);
            log.info("UDP server stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
