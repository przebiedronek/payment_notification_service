package com.biedron.payments.enrichedevents;

import com.biedron.payments.testutils.TestDataBuilder;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.biedron.payments.schema.v1.PaymentEvent;
import com.biedron.payments.schema.v1.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrichedPaymentEventSerDeTest {

    private final EnrichedPaymentEventsSerDe enrichedPaymentEventsSerDe = new EnrichedPaymentEventsSerDe();

    @Test
    public void testSerializationAndDeserialization() {
        Instant createdAt = Instant.parse("2025-03-17T23:00:00.000Z");
        PaymentEvent paymentEvent = TestDataBuilder.createPaymentEvent(
                "pay_789",
                "pay_789-2025-03-17T23:00:00Z",
                createdAt,
                25000L,
                "EUR",
                PaymentStatus.PAYMENT_COMPLETED,
                456L,
                789L
        );

        EnrichedPaymentEvent enrichedPaymentEvent = TestDataBuilder.createEnrichedPaymentEvent(
                paymentEvent,
                TestDataBuilder.createCustomer(456L)
        );

        byte[] enrichedPaymentEventSerialized = enrichedPaymentEventsSerDe.serialize("topic", enrichedPaymentEvent);
        assertThat(enrichedPaymentEventSerialized).isNotEmpty();

        EnrichedPaymentEvent deserializedEvent = enrichedPaymentEventsSerDe.deserialize("topic", enrichedPaymentEventSerialized);
        assertThat(deserializedEvent).isNotNull();

        // Verify PaymentEvent fields
        assertThat(deserializedEvent.getPaymentId()).isEqualTo(enrichedPaymentEvent.getPaymentId());
        assertThat(deserializedEvent.getIdempotencyKey()).isEqualTo(enrichedPaymentEvent.getIdempotencyKey());
        assertThat(deserializedEvent.getPaymentData().getCreatedAt().getSeconds()).isEqualTo(enrichedPaymentEvent.getPaymentData().getCreatedAt().getSeconds());
        assertThat(deserializedEvent.getPaymentData().getCreatedAt().getNanos()).isEqualTo(enrichedPaymentEvent.getPaymentData().getCreatedAt().getNanos());
        assertThat(deserializedEvent.getPaymentData().getAmount()).isEqualTo(enrichedPaymentEvent.getPaymentData().getAmount());
        assertThat(deserializedEvent.getPaymentData().getCurrency()).isEqualTo(enrichedPaymentEvent.getPaymentData().getCurrency());
        assertThat(deserializedEvent.getPaymentData().getPaymentStatus()).isEqualTo(enrichedPaymentEvent.getPaymentData().getPaymentStatus());
        assertThat(deserializedEvent.getCustomerId()).isEqualTo(enrichedPaymentEvent.getCustomerId());
        assertThat(deserializedEvent.getMerchantId()).isEqualTo(enrichedPaymentEvent.getMerchantId());

        // Verify Customer fields
        assertThat(deserializedEvent.getCustomer().getId()).isEqualTo(enrichedPaymentEvent.getCustomer().getId());
        assertThat(deserializedEvent.getCustomer().getEmail()).isEqualTo(enrichedPaymentEvent.getCustomer().getEmail());
        assertThat(deserializedEvent.getCustomer().getName()).isEqualTo(enrichedPaymentEvent.getCustomer().getName());
    }

    @Test
    public void testDeserializeWhenInvalidMessage() {
        byte[] invalidData = "invalid data".getBytes();
        EnrichedPaymentEvent enrichedPaymentEvent = enrichedPaymentEventsSerDe.deserialize("topic", invalidData);
        assertThat(enrichedPaymentEvent).isNull();
    }
}

