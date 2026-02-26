package com.biedron.payments.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookRestServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<String> uriCaptor;

    @Captor
    private ArgumentCaptor<String> bodyCaptor;

    private WebhookRestService webhookRestService;

    private static final String TEST_SUBSCRIPTION_URL = "https://test.example.com/webhook";
    private static final String TEST_JSON_PAYLOAD = """
                {
                  "payment": {
                    "id": "pay_456",
                    "paymentId": "pay_456",
                    "idempotencyKey": "pay_456-2025-03-17T23:00:00Z",
                    "createdAt": "2025-03-17T23:00:00Z",
                    "amount": "25000",
                    "currency": "EUR",
                    "paymentStatus": "PAYMENT_COMPLETED",
                    "customerId": "789",
                    "merchantId": "101112",
                    "originIdempotencyKey": "pay_456-2025-03-17T23:00:00Z"
                  },
                  "customer": {
                    "id": "789",
                    "email": "jane.smith@example.com",
                    "name": "Jane Smith"
                  }
                }
                """;;

    @BeforeEach
    void setUp() {
        webhookRestService = new WebhookRestService(restClient);
    }

    @Test
    void shouldSendWebhookSuccessfully() {
        // Given
        setupSuccessfulRestClientMock();

        // When
        webhookRestService.notifyWebhook(TEST_SUBSCRIPTION_URL, TEST_JSON_PAYLOAD);

        // Then
        verify(restClient).post();
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        verify(requestBodySpec).body(bodyCaptor.capture());
        verify(requestBodySpec).retrieve();

        assertThat(uriCaptor.getValue()).isEqualTo(TEST_SUBSCRIPTION_URL);
        assertThat(bodyCaptor.getValue()).isEqualTo(TEST_JSON_PAYLOAD);
    }

    @Test
    void shouldThrowRetryableExceptionOnHttpError() throws IOException {
        // Given
        String errorBody = "Internal Server Error";
        setupErrorRestClientMock(HttpStatus.INTERNAL_SERVER_ERROR, errorBody);

        // When & Then
        assertThatThrownBy(() -> webhookRestService.notifyWebhook(TEST_SUBSCRIPTION_URL, TEST_JSON_PAYLOAD))
                .isInstanceOf(WebhookRestService.RetryableException.class)
                .hasMessage(errorBody);

        verify(restClient).post();
        verify(requestBodyUriSpec).uri(TEST_SUBSCRIPTION_URL);
        verify(requestBodySpec).body(TEST_JSON_PAYLOAD);
    }

    @Test
    void shouldThrowRetryableExceptionOn4xxError() {
        // Given
        String errorBody = "Bad Request";
        setupErrorRestClientMock(HttpStatus.BAD_REQUEST, errorBody);

        // When & Then
        assertThatThrownBy(() -> webhookRestService.notifyWebhook(TEST_SUBSCRIPTION_URL, TEST_JSON_PAYLOAD))
                .isInstanceOf(WebhookRestService.RetryableException.class)
                .hasMessage(errorBody);
    }

    @Test
    void shouldThrowRetryableExceptionOn5xxError() throws IOException {
        // Given
        String errorBody = "Service Unavailable";
        setupErrorRestClientMock(HttpStatus.SERVICE_UNAVAILABLE, errorBody);

        // When & Then
        assertThatThrownBy(() -> webhookRestService.notifyWebhook(TEST_SUBSCRIPTION_URL, TEST_JSON_PAYLOAD))
                .isInstanceOf(WebhookRestService.RetryableException.class)
                .hasMessage(errorBody);
    }

    @Test
    void shouldThrowRetryableExceptionOnResourceAccessException() {
        // Given
        String exceptionMessage = "Connection timeout";
        setupResourceAccessExceptionMock(exceptionMessage);

        // When & Then
        assertThatThrownBy(() -> webhookRestService.notifyWebhook(TEST_SUBSCRIPTION_URL, TEST_JSON_PAYLOAD))
                .isInstanceOf(WebhookRestService.RetryableException.class)
                .hasMessage(exceptionMessage);

        verify(restClient).post();
    }

    @Test
    void shouldHandleNetworkTimeout() {
        // Given
        String timeoutMessage = "Read timed out";
        setupResourceAccessExceptionMock(timeoutMessage);

        // When & Then
        assertThatThrownBy(() -> webhookRestService.notifyWebhook(TEST_SUBSCRIPTION_URL, TEST_JSON_PAYLOAD))
                .isInstanceOf(WebhookRestService.RetryableException.class)
                .hasMessage(timeoutMessage);
    }

    // Helper methods to setup mocks
    @SuppressWarnings("unchecked")
    private void setupSuccessfulRestClientMock() {
        setupRestClientWithResponse();
        when(responseSpec.onStatus(any(Predicate.class), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
    }

    @SuppressWarnings("unchecked")
    private void setupErrorRestClientMock(HttpStatus errorStatus, String errorBody) {
        setupRestClientWithResponse();

        // Mock the error status handler
        when(responseSpec.onStatus(any(Predicate.class), any())).thenAnswer(invocation -> {
            Predicate<org.springframework.http.HttpStatusCode> predicate = invocation.getArgument(0);
            RestClient.ResponseSpec.ErrorHandler errorHandler = invocation.getArgument(1);

            // Check if this is the error predicate
            if (predicate.test(errorStatus)) {
                // Create a mock response
                RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse mockResponse = mock(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse.class);
                when(mockResponse.getStatusCode()).thenReturn(errorStatus);
                when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8)));

                // Trigger the error handler
                try {
                    errorHandler.handle(null, mockResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return responseSpec;
        });
    }

    private void setupRestClientMock() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
    }

    private void setupRestClientWithResponse() {
        setupRestClientMock();
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    private void setupResourceAccessExceptionMock(String exceptionMessage) {
        setupRestClientMock();
        when(requestBodySpec.retrieve()).thenThrow(new ResourceAccessException(exceptionMessage));
    }
}

