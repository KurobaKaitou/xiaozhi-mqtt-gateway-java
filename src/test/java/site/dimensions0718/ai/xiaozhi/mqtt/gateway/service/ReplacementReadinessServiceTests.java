package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.*;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.UdpServerProperties;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.ReplacementReadinessResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplacementReadinessServiceTests {

    @Test
    void shouldBeNotReadyWhenCriticalChecksFail() {
        Environment environment = Mockito.mock(Environment.class);
        Mockito.when(environment.getProperty(Mockito.eq("gateway.security.signature-key"), ArgumentMatchers.anyString()))
                .thenReturn("");
        Mockito.when(environment.getProperty("MQTT_SIGNATURE_KEY", "")).thenReturn("");

        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.containsBean("mqttInboundAdapter")).thenReturn(false);
        Mockito.when(context.containsBean("mqttOutboundHandler")).thenReturn(false);

        UdpServerProperties udp = new UdpServerProperties();
        udp.setEnabled(false);

        ReplacementReadinessService service = new ReplacementReadinessService(
                environment,
                context,
                udp,
                new NoopAudioRecognizer(),
                new NoopAudioSynthesizer(),
                new NoopRtcPusher(),
                event -> {
                }
        );

        ReplacementReadinessResponse response = service.evaluate();
        assertFalse(response.ready());
    }

    @Test
    void shouldBeReadyWhenCriticalChecksPass() {
        Environment environment = Mockito.mock(Environment.class);
        Mockito.when(environment.getProperty(Mockito.eq("gateway.security.signature-key"), ArgumentMatchers.anyString()))
                .thenReturn("key");
        Mockito.when(environment.getProperty("MQTT_SIGNATURE_KEY", "")).thenReturn("");

        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.containsBean("mqttInboundAdapter")).thenReturn(true);
        Mockito.when(context.containsBean("mqttOutboundHandler")).thenReturn(true);

        UdpServerProperties udp = new UdpServerProperties();
        udp.setEnabled(true);

        IAudioRecognizer recognizer = request -> java.util.Optional.empty();
        IAudioSynthesizer synthesizer = request -> java.util.Optional.empty();
        IRtcPusher rtcPusher = frame -> {
        };
        IBusinessEventPublisher publisher = event -> {
        };

        ReplacementReadinessService service = new ReplacementReadinessService(
                environment,
                context,
                udp,
                recognizer,
                synthesizer,
                rtcPusher,
                publisher
        );

        ReplacementReadinessResponse response = service.evaluate();
        assertTrue(response.ready());
    }
}
