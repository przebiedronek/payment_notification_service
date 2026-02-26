package com.biedron.payments.webhook;

public interface WebhookService {

    void notifyWebhook(String webhookUrl, String eventData);
}

