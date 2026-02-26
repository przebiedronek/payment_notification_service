package com.biedron.payments.enrichedevents;

import com.biedron.payments.testutils.TestDataBuilder;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrichedPaymentEventsProducerTest {

    @Mock
    private KafkaTemplate<Long, EnrichedPaymentEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<Long> keyCaptor;

    @Captor
    private ArgumentCaptor<EnrichedPaymentEvent> eventCaptor;

    private EnrichedPaymentEventsProducer producer;

    private static final String TEST_TOPIC = "enriched.payment.events.test";

    @BeforeEach
    void setUp() {
        producer = new EnrichedPaymentEventsProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "topic", TEST_TOPIC);
    }

    @Test
    void shouldProduceEnrichedPaymentEventWithCustomerIdAsKey() {
        // Given
        Long expectedCustomerId = 123L;
        EnrichedPaymentEvent enrichedPaymentEvent = TestDataBuilder.createEnrichedPaymentEvent(expectedCustomerId, 456L);

        // When
        producer.produce(enrichedPaymentEvent);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(TEST_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo(expectedCustomerId);
        assertThat(eventCaptor.getValue()).isEqualTo(enrichedPaymentEvent);
    }
}

