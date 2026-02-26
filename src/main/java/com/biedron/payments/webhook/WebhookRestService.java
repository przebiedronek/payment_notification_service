package com.biedron.payments.webhook;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatusCode;

import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class WebhookRestService implements WebhookService {

    private final RestClient restClient;

    public WebhookRestService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void notifyWebhook(String subscriptionUrl, String enrichedDataJson) {
        logExecution(subscriptionUrl, enrichedDataJson);

        try {
            restClient.post()
                    .uri(subscriptionUrl)
                    .body(enrichedDataJson)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("Error response from subscription endpoint",
                                kv("statusCode", res.getStatusCode()),
                                kv("errorBody", errorBody),
                                kv("subscriptionUrl", subscriptionUrl));
                        throw new RetryableException(errorBody);
                    }).onStatus(status -> !status.isError(), (req, res) ->
                        log.info("Success response from subscription endpoint",
                                kv("statusCode", res.getStatusCode()),
                                kv("subscriptionUrl", subscriptionUrl))
                    ).toBodilessEntity();
        } catch (ResourceAccessException resourceAccessException) {
            log.warn("Error when calling subscription endpoint", kv("exception", resourceAccessException.getMessage()));
            throw new RetryableException(resourceAccessException.getMessage());
        }
    }

    private void logExecution(String subscriptionUrl, String enrichedDataJson) {
        JSONObject enrichedData = new JSONObject(enrichedDataJson);
        JSONObject customer = enrichedData.optJSONObject("customer");
        log.info("Sending EnrichmentPaymentEvent data to subscription URL",
                kv("subscriptionUrl", subscriptionUrl),
                kv("payment_id", enrichedData.opt("paymentId")),
                kv("customer_id", customer != null ? customer.opt("id") : null),
                kv("idempotencyKey", enrichedData.opt("idempotencyKey")));
    }

    static class RetryableException extends RuntimeException {
        public RetryableException(String message) {
            super(message);
        }
    }

}

