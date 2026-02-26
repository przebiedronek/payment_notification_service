# Payment Notifications Service

A Spring Boot microservice that processes payment events, enriches them with customer data, and delivers webhook notifications to merchant subscription endpoints.

## Overview

The Payment Notifications Service is an event-driven notification system that:
1. Consumes payment events from Kafka (PaymentEventsConsumer)
2. Enriches payment data with customer information from a database (PaymentEnrichmentService)
3. Publishes enriched events to another Kafka topic (EnrichedPaymentEventsProducer)
4. Consumes enriched events from Kafka (EnrichedPaymentEventsConsumer)
5. Delivers webhook notifications to merchant subscription endpoints (WebhookRestService)

This service acts as a bridge between payment processing systems and merchant notification systems, ensuring merchants receive complete payment information including customer details.

## Architecture

### High-Level Architecture

```
┌─────────────────┐
│  Payment Events │
│  Kafka Topic    │
└────────┬────────┘
         │
         ▼
┌────────────────────────────────────────────────┐
│  PaymentEventsConsumer                         │
│  - Consumes PaymentEvent messages              │
│  - Executing enrichement service               │
└────────┬───────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────┐
│  PaymentEnrichmentService              │
│  - Fetches customer data from database │
│  - Creates EnrichedPaymentEvent        │
└────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────────┐
│  EnrichedPaymentEventsProducer                 │
│  - Publishes enriched events to Kafka          │
└────────┬───────────────────────────────────────┘
         │
         ▼
┌─────────────────────┐
│  Enriched Payment   │
│  Events Kafka Topic │
└────────┬────────────┘
         │
         ▼
┌────────────────────────────────────────────────┐
│  EnrichedPaymentEventsConsumer                 │
│  - Consumes enriched events                    │
│  - Retrieves merchant subscription URL         │
│  - Sends webhook notification                  │
└────────┬───────────────────────────────────────┘
         │
         ▼
┌────────────────────┐
│  Merchant Webhook  │
│  Endpoint          │
└────────────────────┘
```


Currently, **MerchantSubscriptionService** is providing static URL. 
Future enhancements will include dynamic subscription URL resolution based on merchant ID.

### Data Models

Kafka messages are definded in Protocol Buffers (protobuf) and can be found in:
src/main/proto/com/biedron/payments/schema/v1/payment_event.proto
src/main/proto/com/biedron/payments/schema/v1/enriched_payment_event.proto

DB consist of customers table, the script to create it can be found in:
src/main/resources/db/migration/V1__create_customers_table.sql
Plan to use Liquibase in the future.


### Technology Stack

- **Framework:** Spring Boot 3.5.0
- **Language:** Java 21
- **Build Tool:** Gradle (Kotlin DSL)
- **Messaging:** Apache Kafka with Spring Kafka
- **Database:** PostgreSQL (production), H2 (testing)
- **ORM:** Spring Data JPA with Hibernate
- **Serialization:** Protocol Buffers (protobuf)
- **HTTP Client:** Spring RestClient
- **Logging:** Logback with Logstash encoder for structured logging
- **Testing:** JUnit 5, Mockito, Testcontainers, WireMock

### Configuration

#### Required Environment Variables

| Variable                        | Description                        |
|---------------------------------|------------------------------------|
| `DB_CONNECTION_URL`             | Database connection URL            |
| `DB_USERNAME`                   | Database username                  |
| `DB_PASSWORD`                   | Database password                  |
| `KAFKA_BOOTSTRAP_URL`           | Kafka bootstrap servers            |
| `KAFKA_API_KEY`                 | Kafka SASL authentication username |
| `KAFKA_API_SECRET`              | Kafka SASL authentication password |
| `KAFKA_PAYMENT_CONSUMER_GROUP`  | Consumer group for payment events  |
| `KAFKA_ENRICHED_CONSUMER_GROUP` | Consumer group for enriched events |
| `WEBHOOK_API_KEY`               | API key for webhook authentication |

#### Application Properties

Key configurations in `application.yaml`:

- **Virtual Threads:** Enabled for improved concurrency (`spring.threads.virtual.enabled: true`)
- **Kafka Topics:**
  - Payment events: `payment.events`
  - Enriched payment events: `enriched.payment.events`
- **Kafka Producer:**
  - **Idempotence:** Enabled (`enable.idempotence: true`)
- **REST Client:**
  - Timeout: 15000ms
  - Retry: Max 3 attempts with exponential backoff (initial delay: 1000ms, multiplier: 2)

### Error Handling & Resilience

1. **Kafka Consumer Error Handling:**
   - Exponential backoff retry policy for enriched events consumer
   - Configurable max retry attempts and delay multiplier
   - Future improvements: Dead-letter queue for failed events

2. **Webhook Delivery:**
   - Retry mechanism with exponential backoff

### Idempotence & Deduplication

The system implements **Kafka's built-in idempotent producer** to prevent duplicate messages (`enable.idempotence: true`) 

The `idempotency_key` field in `PaymentEvent` is kept for:
- Audit trail - Track the original request that created the payment
- End-to-end tracing - Correlate events across systems
- Merchant-side deduplication - Merchants can use it for their own deduplication


## Getting Started

### Prerequisites

- Java 21
- Gradle 8.x
- PostgreSQL database
- Kafka cluster (Confluent Platform or Apache Kafka)


### Tests

Tests use:
- **Testcontainers** for Kafka integration tests. To run the tests, you will need to have Docker installed and running on your machine. The tests will automatically start a Kafka container when executed.
- **H2** in-memory database for JPA tests
- **WireMock** for mocking webhook endpoints

## Future improvements
- **DLQ** for failed events
- **Caching** for customer data
- **Merchant service** to resolve subscription URL dynamically based on merchant ID


