package com.biedron.payments.paymentevents;

import com.biedron.payments.testutils.TestDataBuilder;
import com.biedron.payments.enrichedevents.EnrichedPaymentEventsProducer;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.biedron.payments.schema.v1.PaymentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentEventsConsumerTest {

    @Mock
    private PaymentEnrichmentService paymentEnrichmentService;

    @Mock
    private EnrichedPaymentEventsProducer enrichedPaymentEventsProducer;

    private PaymentEventsConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentEventsConsumer(paymentEnrichmentService, enrichedPaymentEventsProducer);
    }

    @Test
    void shouldProcessPaymentEventSuccessfully() {
        PaymentEvent paymentEvent = TestDataBuilder.createPaymentEvent(123L, 456L);
        EnrichedPaymentEvent enrichedPaymentEvent = TestDataBuilder.createEnrichedPaymentEvent(paymentEvent);

        when(paymentEnrichmentService.enrich(paymentEvent)).thenReturn(enrichedPaymentEvent);

        consumer.consume(paymentEvent);

        verify(paymentEnrichmentService).enrich(paymentEvent);
        verify(enrichedPaymentEventsProducer).produce(enrichedPaymentEvent);
    }

    @Test
    void shouldDelegateEnrichmentToService() {
        PaymentEvent paymentEvent = TestDataBuilder.createPaymentEvent(123L, 456L);
        EnrichedPaymentEvent enrichedPaymentEvent = TestDataBuilder.createEnrichedPaymentEvent(paymentEvent);

        when(paymentEnrichmentService.enrich(paymentEvent)).thenReturn(enrichedPaymentEvent);

        consumer.consume(paymentEvent);

        verify(paymentEnrichmentService).enrich(paymentEvent);
        verify(enrichedPaymentEventsProducer).produce(argThat(enrichedEvent -> {
            assertThat(enrichedEvent.getPaymentId()).isEqualTo(paymentEvent.getPaymentId());
            assertThat(enrichedEvent.getCustomerId()).isEqualTo(paymentEvent.getCustomerId());
            assertThat(enrichedEvent.getMerchantId()).isEqualTo(paymentEvent.getMerchantId());
            assertThat(enrichedEvent.getCustomer().getId()).isEqualTo(123L);
            assertThat(enrichedEvent.getCustomer().getEmail()).isEqualTo(TestDataBuilder.getDefaultEmail());
            assertThat(enrichedEvent.getCustomer().getName()).isEqualTo(TestDataBuilder.getDefaultName());
            return true;
        }));
    }
}

