package com.biedron.payments.enrichedevents;

import com.biedron.payments.webhook.WebhookService;
import com.biedron.payments.webhook.SubscriptionService;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RequiredArgsConstructor
@Component
public class EnrichedPaymentEventsConsumer {

    private final SubscriptionService subscriptionService;

    private final WebhookService webhookService;

    @KafkaListener(
            topics = "${spring.application.app.topics.enriched-payment-events}",
            groupId = "${KAFKA_ENRICHED_CONSUMER_GROUP}",
            containerFactory = "enrichedPaymentEventsKafkaListenerContainerFactory"
    )
    public void consume(EnrichedPaymentEvent enrichedPaymentEvent) {
        log.info("Received EnrichedPaymentEvent from Kafka",
                kv("payment_id", enrichedPaymentEvent.getPaymentId()),
                kv("status", enrichedPaymentEvent.getPaymentData().getPaymentStatus()),
                kv("customer_id", enrichedPaymentEvent.getCustomerId()));

        try {
            String enrichedPaymentJson = JsonFormat.printer().print(enrichedPaymentEvent.toBuilder().build());
            String subscriptionUrl = subscriptionService.getSubscriptionForEvent(enrichedPaymentEvent.getMerchantId());
            webhookService.notifyWebhook(subscriptionUrl, enrichedPaymentJson);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error converting EnrichedPaymentEvent to JSON",
                    kv("exception", e.getMessage()),
                    kv("stacktrace", e.getStackTrace()));
        }
    }

}

