package com.biedron.payments.paymentevents;

import com.biedron.payments.enrichedevents.EnrichedPaymentEventsProducer;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.biedron.payments.schema.v1.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventsConsumer {

    private final PaymentEnrichmentService paymentEnrichmentService;

    private final EnrichedPaymentEventsProducer enrichedPaymentEventsProducer;

    @KafkaListener(
            topics = "${spring.application.app.topics.payment-events}",
            groupId = "${KAFKA_PAYMENT_CONSUMER_GROUP}",
            containerFactory = "paymentEventsKafkaListenerContainerFactory"
    )
    public void consume(PaymentEvent paymentEvent) {
        log.info("Received PaymentEvent from Kafka topic",
                kv("payment_id", paymentEvent.getPaymentId()),
                kv("status", paymentEvent.getPaymentData().getPaymentStatus()),
                kv("amount", paymentEvent.getPaymentData().getAmount()),
                kv("currency", paymentEvent.getPaymentData().getCurrency()),
                kv("customer_id", paymentEvent.getCustomerId()),
                kv("merchant_id", paymentEvent.getMerchantId()),
                kv("idempotency_key", paymentEvent.getIdempotencyKey()));

        EnrichedPaymentEvent enrichedPaymentEvent = paymentEnrichmentService.enrich(paymentEvent);

        enrichedPaymentEventsProducer.produce(enrichedPaymentEvent);
    }
}

