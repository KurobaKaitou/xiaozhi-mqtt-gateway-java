package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.*;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.UdpServerProperties;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.ReadinessCheckItem;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.web.dto.ReplacementReadinessResponse;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReplacementReadinessService {

    private final Environment environment;
    private final ApplicationContext applicationContext;
    private final UdpServerProperties udpServerProperties;
    private final IAudioRecognizer audioRecognizer;
    private final IAudioSynthesizer audioSynthesizer;
    private final IRtcPusher rtcPusher;
    private final IBusinessEventPublisher businessEventPublisher;

    public ReplacementReadinessService(
            Environment environment,
            ApplicationContext applicationContext,
            UdpServerProperties udpServerProperties,
            IAudioRecognizer audioRecognizer,
            IAudioSynthesizer audioSynthesizer,
            IRtcPusher rtcPusher,
            IBusinessEventPublisher businessEventPublisher
    ) {
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.udpServerProperties = udpServerProperties;
        this.audioRecognizer = audioRecognizer;
        this.audioSynthesizer = audioSynthesizer;
        this.rtcPusher = rtcPusher;
        this.businessEventPublisher = businessEventPublisher;
    }

    private static ReadinessCheckItem check(String code, boolean passed, String detail, boolean critical) {
        return new ReadinessCheckItem(code, passed, detail, critical);
    }

    public ReplacementReadinessResponse evaluate() {
        List<ReadinessCheckItem> checks = new ArrayList<>();

        String signatureKey = environment.getProperty("gateway.security.signature-key",
                environment.getProperty("MQTT_SIGNATURE_KEY", ""));
        checks.add(check(
                "SIGNATURE_KEY_CONFIGURED",
                signatureKey != null && !signatureKey.isBlank(),
                "gateway.security.signature-key or MQTT_SIGNATURE_KEY must be configured",
                true
        ));

        checks.add(check(
                "UDP_SERVER_ENABLED",
                udpServerProperties.isEnabled(),
                "gateway.udp.enabled should be true in production replacement mode",
                true
        ));

        boolean hasMqttInbound = applicationContext.containsBean("mqttInboundAdapter");
        checks.add(check(
                "MQTT_INBOUND_ADAPTER_PRESENT",
                hasMqttInbound,
                "No MQTT inbound adapter bean found",
                true
        ));

        boolean hasMqttOutbound = applicationContext.containsBean("mqttOutboundHandler");
        checks.add(check(
                "MQTT_OUTBOUND_ADAPTER_PRESENT",
                hasMqttOutbound,
                "No MQTT outbound adapter bean found",
                true
        ));

        checks.add(check(
                "AUDIO_RECOGNIZER_CUSTOMIZED",
                !(audioRecognizer instanceof NoopAudioRecognizer),
                "No custom ASR adapter configured; currently noop",
                false
        ));

        checks.add(check(
                "AUDIO_SYNTHESIZER_CUSTOMIZED",
                !(audioSynthesizer instanceof NoopAudioSynthesizer),
                "No custom TTS adapter configured; currently noop",
                false
        ));

        checks.add(check(
                "RTC_PUSHER_CUSTOMIZED",
                !(rtcPusher instanceof NoopRtcPusher),
                "No custom RTC pusher configured; currently noop",
                false
        ));

        checks.add(check(
                "BUSINESS_EVENT_PUBLISHER_PRESENT",
                businessEventPublisher != null,
                "Business event publisher bean missing",
                false
        ));

        boolean ready = checks.stream().filter(ReadinessCheckItem::critical).allMatch(ReadinessCheckItem::passed);
        return new ReplacementReadinessResponse(ready, List.copyOf(checks));
    }

}
