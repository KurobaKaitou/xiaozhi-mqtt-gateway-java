package site.dimensions0718.ai.xiaozhi.mqtt.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge.*;

@Configuration
public class BridgeDefaultAdaptersConfig {

    @Bean
    @ConditionalOnMissingBean(IAudioRecognizer.class)
    public IAudioRecognizer audioRecognizer() {
        return new NoopAudioRecognizer();
    }

    @Bean
    @ConditionalOnMissingBean(IAudioSynthesizer.class)
    public IAudioSynthesizer audioSynthesizer() {
        return new NoopAudioSynthesizer();
    }

    @Bean
    @ConditionalOnMissingBean(IRtcPusher.class)
    public IRtcPusher rtcPusher() {
        return new NoopRtcPusher();
    }

    @Bean
    @ConditionalOnMissingBean(IBusinessEventPublisher.class)
    public IBusinessEventPublisher businessEventPublisher() {
        return new LoggingBusinessEventPublisher();
    }
}
