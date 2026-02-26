package com.biedron.payments.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantSubscriptionServiceTest {

    private MerchantSubscriptionService subscriptionService;

    private static final String TEST_SUBSCRIPTION_URL = "https://test.example.com/webhook";

    @BeforeEach
    void setUp() {
        subscriptionService = new MerchantSubscriptionService();
        ReflectionTestUtils.setField(subscriptionService, "gnsSubscriptionUrl", TEST_SUBSCRIPTION_URL);
    }

    @Test
    void shouldReturnConfiguredSubscriptionUrl() {
        // Given
        Long merchantId = 123L;

        // When
        String subscriptionUrl = subscriptionService.getSubscriptionForEvent(merchantId);

        // Then
        assertThat(subscriptionUrl).isEqualTo(TEST_SUBSCRIPTION_URL);
    }

    @Test
    void shouldReturnUrlForNullMerchantId() {
        // Given
        Long merchantId = null;

        // When
        String subscriptionUrl = subscriptionService.getSubscriptionForEvent(merchantId);

        // Then
        assertThat(subscriptionUrl).isEqualTo(TEST_SUBSCRIPTION_URL);
    }

}

