package com.biedron.payments.testutils;

import com.biedron.payments.customer.CustomerDto;
import com.biedron.payments.schema.v1.Customer;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.biedron.payments.schema.v1.PaymentData;
import com.biedron.payments.schema.v1.PaymentEvent;
import com.biedron.payments.schema.v1.PaymentStatus;
import com.google.protobuf.Timestamp;

import java.time.Instant;

/**
 * Utility class for creating test data objects used across multiple test classes.
 * Provides builder methods for PaymentEvent, EnrichedPaymentEvent, Customer, and CustomerDto.
 */
public class TestDataBuilder {

    private static final Instant DEFAULT_CREATED_AT = Instant.parse("2025-03-17T23:00:00.000Z");
    private static final String DEFAULT_PAYMENT_ID = "pay_123";
    private static final String DEFAULT_IDEMPOTENCY_KEY = "pay_123-2025-02-22T10:15:30Z";
    private static final long DEFAULT_AMOUNT = 10000L;
    private static final String DEFAULT_CURRENCY = "USD";
    private static final PaymentStatus DEFAULT_STATUS = PaymentStatus.PAYMENT_COMPLETED;
    private static final Long DEFAULT_CUSTOMER_ID = 123L;
    private static final Long DEFAULT_MERCHANT_ID = 456L;
    private static final String DEFAULT_EMAIL = "john.doe@example.com";
    private static final String DEFAULT_NAME = "John Doe";


    /**
     * Creates a PaymentEvent with specified customer and merchant IDs.
     */
    public static PaymentEvent createPaymentEvent(Long customerId, Long merchantId) {
        return createPaymentEvent(DEFAULT_PAYMENT_ID, customerId, merchantId);
    }

    /**
     * Creates a PaymentEvent with specified payment ID, customer ID, and merchant ID.
     */
    public static PaymentEvent createPaymentEvent(String paymentId, Long customerId, Long merchantId) {
        return createPaymentEvent(
                paymentId,
                paymentId + "-2025-02-22T10:15:30Z",
                DEFAULT_CREATED_AT,
                DEFAULT_AMOUNT,
                DEFAULT_CURRENCY,
                DEFAULT_STATUS,
                customerId,
                merchantId
        );
    }

    /**
     * Creates a PaymentEvent with all customizable fields.
     */
    public static PaymentEvent createPaymentEvent(
            String paymentId,
            String idempotencyKey,
            Instant createdAt,
            long amount,
            String currency,
            PaymentStatus status,
            Long customerId,
            Long merchantId) {

        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(createdAt.getEpochSecond())
                .setNanos(createdAt.getNano())
                .build();

        PaymentData paymentData = PaymentData.newBuilder()
                .setAmount(amount)
                .setCurrency(currency)
                .setPaymentStatus(status)
                .setCreatedAt(timestamp)
                .build();

        return PaymentEvent.newBuilder()
                .setPaymentId(paymentId)
                .setIdempotencyKey(idempotencyKey)
                .setCustomerId(customerId)
                .setMerchantId(merchantId)
                .setPaymentData(paymentData)
                .build();
    }

    /**
     * Creates a Customer with specified customer ID.
     */
    public static Customer createCustomer(Long customerId) {
        return createCustomer(customerId, DEFAULT_EMAIL, DEFAULT_NAME);
    }

    /**
     * Creates a Customer with all customizable fields.
     */
    public static Customer createCustomer(Long customerId, String email, String name) {
        return Customer.newBuilder()
                .setId(customerId)
                .setEmail(email)
                .setName(name)
                .build();
    }

    /**
     * Creates an EnrichedPaymentEvent with default values.
     */
    public static EnrichedPaymentEvent createEnrichedPaymentEvent() {
        return createEnrichedPaymentEvent(DEFAULT_CUSTOMER_ID, DEFAULT_MERCHANT_ID);
    }

    /**
     * Creates an EnrichedPaymentEvent with specified customer and merchant IDs.
     */
    public static EnrichedPaymentEvent createEnrichedPaymentEvent(Long customerId, Long merchantId) {
        PaymentEvent paymentEvent = createPaymentEvent(customerId, merchantId);
        Customer customer = createCustomer(customerId);
        return createEnrichedPaymentEvent(paymentEvent, customer);
    }

    /**
     * Creates an EnrichedPaymentEvent from a PaymentEvent with default customer data.
     */
    public static EnrichedPaymentEvent createEnrichedPaymentEvent(PaymentEvent paymentEvent) {
        Customer customer = createCustomer(paymentEvent.getCustomerId());
        return createEnrichedPaymentEvent(paymentEvent, customer);
    }

    /**
     * Creates an EnrichedPaymentEvent with specified PaymentEvent and Customer.
     */
    public static EnrichedPaymentEvent createEnrichedPaymentEvent(PaymentEvent paymentEvent, Customer customer) {
        return EnrichedPaymentEvent.newBuilder()
                .setPaymentId(paymentEvent.getPaymentId())
                .setIdempotencyKey(paymentEvent.getIdempotencyKey())
                .setCustomerId(paymentEvent.getCustomerId())
                .setMerchantId(paymentEvent.getMerchantId())
                .setPaymentData(paymentEvent.getPaymentData())
                .setCustomer(customer)
                .build();
    }


    /**
     * Creates a CustomerDto with specified customer ID.
     */
    public static CustomerDto createCustomerDto(Long customerId) {
        return createCustomerDto(customerId, DEFAULT_EMAIL, DEFAULT_NAME);
    }

    /**
     * Creates a CustomerDto with all customizable fields.
     */
    public static CustomerDto createCustomerDto(Long customerId, String email, String name) {
        return CustomerDto.builder()
                .customerId(customerId)
                .customerEmail(email)
                .customerName(name)
                .build();
    }

    /**
     * Returns the default created at timestamp.
     */
    public static Instant getDefaultCreatedAt() {
        return DEFAULT_CREATED_AT;
    }

    /**
     * Returns the default email.
     */
    public static String getDefaultEmail() {
        return DEFAULT_EMAIL;
    }

    /**
     * Returns the default name.
     */
    public static String getDefaultName() {
        return DEFAULT_NAME;
    }
}

