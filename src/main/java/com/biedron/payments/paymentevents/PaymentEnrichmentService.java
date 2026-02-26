package com.biedron.payments.paymentevents;

import com.biedron.payments.customer.CustomerDto;
import com.biedron.payments.customer.CustomerService;
import com.biedron.payments.schema.v1.Customer;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.biedron.payments.schema.v1.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEnrichmentService {

    private final CustomerService customerService;

    public EnrichedPaymentEvent enrich(PaymentEvent paymentEvent) {
        log.debug("Enriching payment event with customer data",
                kv("payment_id", paymentEvent.getPaymentId()),
                kv("customer_id", paymentEvent.getCustomerId()));

        CustomerDto customerDto = customerService.getCustomerData(paymentEvent.getCustomerId());

        Customer customer = Customer.newBuilder()
                .setId(customerDto.getCustomerId())
                .setEmail(customerDto.getCustomerEmail())
                .setName(customerDto.getCustomerName())
                .build();

        EnrichedPaymentEvent enrichedPaymentEvent = EnrichedPaymentEvent.newBuilder()
                .setPaymentId(paymentEvent.getPaymentId())
                .setIdempotencyKey(paymentEvent.getIdempotencyKey())
                .setCustomerId(paymentEvent.getCustomerId())
                .setMerchantId(paymentEvent.getMerchantId())
                .setPaymentData(paymentEvent.getPaymentData())
                .setCustomer(customer)
                .build();

        log.debug("Successfully enriched payment event",
                kv("payment_id", paymentEvent.getPaymentId()),
                kv("customer_id", customerDto.getCustomerId()),
                kv("customer_email", customerDto.getCustomerEmail()));

        return enrichedPaymentEvent;
    }
}

