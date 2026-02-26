package com.biedron.payments.enrichedevents;

import com.biedron.payments.testutils.TestDataBuilder;
import com.biedron.payments.webhook.WebhookService;
import com.biedron.payments.webhook.SubscriptionService;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnrichedPaymentEventsConsumerTest {

    @Mock
    private WebhookService webhookService;
    @Mock
    private SubscriptionService subscriptionService;

    private static final String SUBSCRIPTION_URL = "http://example.com/webhook";
    private EnrichedPaymentEventsConsumer consumer;

    @BeforeEach
    public void setup() {
        consumer = new EnrichedPaymentEventsConsumer(subscriptionService, webhookService);
    }

    @Test
    public void testConsumeSuccessfully() throws InvalidProtocolBufferException {
        when(subscriptionService.getSubscriptionForEvent(456L))
                .thenReturn(SUBSCRIPTION_URL);
        EnrichedPaymentEvent event = TestDataBuilder.createEnrichedPaymentEvent();
        String eventJson = JsonFormat.printer().print(event.toBuilder().build());

        consumer.consume(event);

        verify(webhookService).notifyWebhook(SUBSCRIPTION_URL, eventJson);
    }

    @Test
    public void testConsumeWhenInvalidMessage() throws InvalidProtocolBufferException {
        EnrichedPaymentEvent event = TestDataBuilder.createEnrichedPaymentEvent();

        try (MockedStatic<JsonFormat> jsonFormatMock = mockStatic(JsonFormat.class)) {
            JsonFormat.Printer printerMock = mock(JsonFormat.Printer.class);
            jsonFormatMock.when(JsonFormat::printer).thenReturn(printerMock);
            when(printerMock.print(any(EnrichedPaymentEvent.class))).thenThrow(new InvalidProtocolBufferException("malformed"));

            consumer.consume(event);

            verifyNoInteractions(subscriptionService);
            verifyNoInteractions(webhookService);
        }
    }
}

