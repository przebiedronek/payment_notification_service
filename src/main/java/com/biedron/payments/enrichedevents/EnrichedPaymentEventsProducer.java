package com.biedron.payments.enrichedevents;

import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RequiredArgsConstructor
@Service
public class EnrichedPaymentEventsProducer {

    @Value("${spring.application.app.topics.enriched-payment-events}")
    private String topic;

    private final KafkaTemplate<Long, EnrichedPaymentEvent> kafkaTemplate;

    public void produce(EnrichedPaymentEvent enrichedPaymentEvent) {
        Long customerId = enrichedPaymentEvent.getCustomerId();

        log.info("Sending enrichedPaymentEvent to topic",
                kv("topic", topic),
                kv("customer_id", customerId));

        kafkaTemplate.send(topic, customerId, enrichedPaymentEvent);
    }
}

