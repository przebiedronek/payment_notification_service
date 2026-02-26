package com.biedron.payments.paymentevents;

import com.biedron.payments.testutils.TestDataBuilder;
import com.biedron.payments.schema.v1.PaymentEvent;
import com.biedron.payments.schema.v1.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentEventSerDeTest {

    private final PaymentEventsSerDe paymentEventsSerDe = new PaymentEventsSerDe();

    @Test
    public void testSerializationAndDeserialization() {
        Instant createdAt = TestDataBuilder.getDefaultCreatedAt();
        PaymentEvent paymentEvent = TestDataBuilder.createPaymentEvent("pay_456", 123L, 456L);

        byte[] paymentEventSerialized = paymentEventsSerDe.serialize("topic", paymentEvent);
        assertThat(paymentEventSerialized).isNotEmpty();

        PaymentEvent paymentEventDeserialized = paymentEventsSerDe.deserialize("topic", paymentEventSerialized);
        assertThat(paymentEventDeserialized.getPaymentId()).isEqualTo("pay_456");
        assertThat(paymentEventDeserialized.getIdempotencyKey()).isEqualTo("pay_456-2025-02-22T10:15:30Z");
        assertThat(paymentEventDeserialized.getPaymentData().getCreatedAt().getSeconds()).isEqualTo(createdAt.getEpochSecond());
        assertThat(paymentEventDeserialized.getPaymentData().getCreatedAt().getNanos()).isEqualTo(createdAt.getNano());
        assertThat(paymentEventDeserialized.getPaymentData().getAmount()).isEqualTo(10000L);
        assertThat(paymentEventDeserialized.getPaymentData().getCurrency()).isEqualTo("USD");
        assertThat(paymentEventDeserialized.getPaymentData().getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_COMPLETED);
        assertThat(paymentEventDeserialized.getCustomerId()).isEqualTo(123L);
        assertThat(paymentEventDeserialized.getMerchantId()).isEqualTo(456L);
    }

    @Test
    public void testDeserializeWhenInvalidMessage() {
        byte[] invalidData = "invalid data".getBytes();
        PaymentEvent paymentEvent = paymentEventsSerDe.deserialize("topic", invalidData);
        assertThat(paymentEvent).isNull();
    }
}

