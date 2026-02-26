package com.biedron.payments.testutils;

import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.core.ConditionTimeoutException;

import static org.awaitility.Awaitility.await;

@Getter
@Slf4j
@Component
@Profile("test")
public class EnrichedPaymentEventsConsumerHelper {

    private final List<EnrichedPaymentEvent> receivedEvents = new ArrayList<>();
    
    @KafkaListener(
            topics = "${spring.application.app.topics.enriched-payment-events}",
            groupId = "test-enriched-payment-consumer",
            containerFactory = "enrichedPaymentEventsKafkaListenerContainerFactory"
    )
    public void consume(EnrichedPaymentEvent enrichedPaymentEvent) {
        log.info("Received enriched payment event: {}", enrichedPaymentEvent);
        receivedEvents.add(enrichedPaymentEvent);
    }
    
    public boolean awaitEvent(long timeoutSeconds) {
        try {
            await()
                    .atMost(Duration.ofSeconds(timeoutSeconds))
                    .until(() -> !receivedEvents.isEmpty());
            return true;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }
    
    public void reset() {
        receivedEvents.clear();
    }
}

