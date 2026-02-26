package com.biedron.payments.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MerchantSubscriptionService implements SubscriptionService {

    @Value("${spring.application.app.subscriptions.url}")
    private String gnsSubscriptionUrl;

    @Override
    public String getSubscriptionForEvent(Long merchantId) {
        return gnsSubscriptionUrl;
    }
}

