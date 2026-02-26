package com.biedron.payments.paymentevents;

import com.biedron.payments.testutils.TestDataBuilder;
import com.biedron.payments.customer.CustomerDto;
import com.biedron.payments.customer.CustomerNotFoundException;
import com.biedron.payments.customer.CustomerService;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.biedron.payments.schema.v1.PaymentEvent;
import com.biedron.payments.schema.v1.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEnrichmentServiceTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private PaymentEnrichmentService paymentEnrichmentService;

    @Test
    void shouldEnrichPaymentEventWithCustomerData() {
        // Given
        PaymentEvent paymentEvent = TestDataBuilder.createPaymentEvent("pay_456", 123L, 456L);
        CustomerDto customerDto = TestDataBuilder.createCustomerDto(123L);

        when(customerService.getCustomerData(123L)).thenReturn(customerDto);

        // When
        EnrichedPaymentEvent result = paymentEnrichmentService.enrich(paymentEvent);

        // Then
        assertThat(result).isNotNull();

        assertThat(result.getPaymentId()).isEqualTo("pay_456");
        assertThat(result.getIdempotencyKey()).isEqualTo("pay_456-2025-02-22T10:15:30Z");
        assertThat(result.getPaymentData().getAmount()).isEqualTo(10000L);
        assertThat(result.getPaymentData().getCurrency()).isEqualTo("USD");
        assertThat(result.getPaymentData().getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_COMPLETED);
        assertThat(result.getCustomerId()).isEqualTo(123L);
        assertThat(result.getMerchantId()).isEqualTo(456L);

        assertThat(result.getCustomer().getId()).isEqualTo(123L);
        assertThat(result.getCustomer().getEmail()).isEqualTo(TestDataBuilder.getDefaultEmail());
        assertThat(result.getCustomer().getName()).isEqualTo(TestDataBuilder.getDefaultName());

        verify(customerService).getCustomerData(123L);
    }

    @Test
    void shouldPropagateCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
        // Given
        PaymentEvent paymentEvent = TestDataBuilder.createPaymentEvent(999L, 456L);

        when(customerService.getCustomerData(999L))
                .thenThrow(new CustomerNotFoundException("Customer not found: 999"));

        // When & Then
        assertThatThrownBy(() -> paymentEnrichmentService.enrich(paymentEvent))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found: 999");

        verify(customerService).getCustomerData(999L);
    }
}

