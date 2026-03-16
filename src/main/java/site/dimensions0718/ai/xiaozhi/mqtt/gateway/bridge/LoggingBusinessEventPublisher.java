package site.dimensions0718.ai.xiaozhi.mqtt.gateway.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingBusinessEventPublisher implements IBusinessEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingBusinessEventPublisher.class);

    @Override
    public void publishAudioInteraction(AudioInteractionEvent event) {
        log.info("audio interaction published: clientId={}, mac={}, sequence={}, payloadBytes={}",
                event.clientId(), event.macAddress(), event.sequence(), event.payloadBytes());
    }
}
