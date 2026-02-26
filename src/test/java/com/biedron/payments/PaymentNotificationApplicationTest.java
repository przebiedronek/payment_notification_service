package com.biedron.payments;

import com.biedron.payments.customer.Customer;
import com.biedron.payments.customer.CustomerRepository;
import com.biedron.payments.testutils.EnrichedPaymentEventsConsumerHelper;
import com.biedron.payments.testutils.TestDataBuilder;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.biedron.payments.schema.v1.PaymentEvent;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import jakarta.persistence.EntityManager;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PaymentNotificationApplicationTest {

    private static final String SUBSCRIPTION = "/subscription";

    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withEnv("KAFKA_HEAP_OPTS", "-Xmx256m")
            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forLogMessage(".*started \\(kafka.server.Kafka.*Server\\).*", 1))
            .withStartupTimeout(Duration.ofMinutes(1));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("KAFKA_PAYMENT_CONSUMER_GROUP", () -> "test-consumer-group-payment");
        registry.add("KAFKA_ENRICHED_CONSUMER_GROUP", () -> "test-consumer-group-enriched");
        registry.add("spring.application.app.subscriptions.url", () -> wiremock.baseUrl() + "/subscription");
    }

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Value("${spring.application.app.topics.payment-events}")
    private String paymentEventsTopic;

    @Value("${spring.application.app.rest.blocking.retry.maxAttempts}")
    private int maxAttempts;

    @Value("${spring.application.app.rest.timeout}")
    private int timeout;

    @Value("${KAFKA_ENRICHED_CONSUMER_GROUP}")
    private String enrichedConsumerGroupId;

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    private final EnrichedPaymentEventsConsumerHelper enrichedPaymentEventsConsumerHelper;

    private final CustomerRepository customerRepository;

    private Long savedCustomerId;
    private Long savedMerchantId;

    @BeforeEach
    @Transactional
    void setUp() {
        enrichedPaymentEventsConsumerHelper.reset();
        wiremock.resetAll();

        insertDataForTest();
    }

    @AfterEach
    @Transactional
    public void cleanData() {
        customerRepository.deleteAll();
    }

    private void insertDataForTest() {
        Customer customer = new Customer();
        customer.setId(123L);
        customer.setEmail("john.doe@example.com");
        customer.setName("John Doe");
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        customer = customerRepository.save(customer);
        savedCustomerId = customer.getId();
        savedMerchantId = 456L;
    }

    @Test
    public void shouldProcessEventSuccessfully() {
        stubHttpResponses(List.of(ResponseStub.SUCCESS));

        PaymentEvent paymentEvent = createSamplePaymentEvent();
        kafkaTemplate.send(paymentEventsTopic, paymentEvent);

        boolean messageReceived = enrichedPaymentEventsConsumerHelper.awaitEvent(5);
        assertThat(messageReceived).isTrue();
        assertThat(enrichedPaymentEventsConsumerHelper.getReceivedEvents()).hasSize(1);

        EnrichedPaymentEvent receivedEvent = enrichedPaymentEventsConsumerHelper.getReceivedEvents().getFirst();
        compareEvents(receivedEvent, paymentEvent);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                wiremock.verify(postRequestedFor(urlEqualTo(SUBSCRIPTION))
                        .withRequestBody(equalToJson(jsonBody()))
                        .withHeader("Content-Type", equalTo("application/json"))
                        .withHeader("X-API-Key", equalTo("test-api-key-12345")))
        );

        awaitCommittedOffsetsAtLogEnd(enrichedConsumerGroupId);
    }

    @Test
    public void shouldRetryTimeout() {
        stubHttpResponsesWithTimeouts();

        PaymentEvent paymentEvent = createSamplePaymentEvent();
        kafkaTemplate.send(paymentEventsTopic, paymentEvent);

        boolean messageReceived = enrichedPaymentEventsConsumerHelper.awaitEvent(5);
        assertThat(messageReceived).isTrue();
        assertThat(enrichedPaymentEventsConsumerHelper.getReceivedEvents()).hasSize(1);

        EnrichedPaymentEvent receivedEvent = enrichedPaymentEventsConsumerHelper.getReceivedEvents().getFirst();
        compareEvents(receivedEvent, paymentEvent);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                wiremock.verify(2, postRequestedFor(urlEqualTo(SUBSCRIPTION))
                        .withRequestBody(equalToJson(jsonBody()))
                        .withHeader("Content-Type", equalTo("application/json"))
                        .withHeader("X-API-Key", equalTo("test-api-key-12345")))
        );

        awaitCommittedOffsetsAtLogEnd(enrichedConsumerGroupId);
    }


    @Test
    public void shouldExhaustNumberOfRetries() {
        int retryCount = maxAttempts+1;
        stubHttpResponses(IntStream.range(0, retryCount).boxed().map(x -> ResponseStub.ERROR).toList());

        PaymentEvent paymentEvent = createSamplePaymentEvent();
        kafkaTemplate.send(paymentEventsTopic, paymentEvent);

        boolean messageReceived = enrichedPaymentEventsConsumerHelper.awaitEvent(5);
        assertThat(messageReceived).isTrue();
        assertThat(enrichedPaymentEventsConsumerHelper.getReceivedEvents()).hasSize(1);

        EnrichedPaymentEvent receivedEvent = enrichedPaymentEventsConsumerHelper.getReceivedEvents().getFirst();
        assertThat(receivedEvent.getPaymentId()).isEqualTo(paymentEvent.getPaymentId());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                wiremock.verify(retryCount, postRequestedFor(urlEqualTo(SUBSCRIPTION))
                        .withRequestBody(equalToJson(jsonBody()))
        ));

        awaitCommittedOffsetsAtLogEnd(enrichedConsumerGroupId);
    }

    @Test
    public void shouldSucceedAfterFailure() {
        stubHttpResponses(List.of(ResponseStub.ERROR, ResponseStub.SUCCESS));

        PaymentEvent paymentEvent = createSamplePaymentEvent();
        kafkaTemplate.send(paymentEventsTopic, paymentEvent);

        boolean messageReceived = enrichedPaymentEventsConsumerHelper.awaitEvent(1);
        assertThat(messageReceived).isTrue();
        assertThat(enrichedPaymentEventsConsumerHelper.getReceivedEvents()).hasSize(1);

        EnrichedPaymentEvent receivedEvent = enrichedPaymentEventsConsumerHelper.getReceivedEvents().getFirst();
        assertThat(receivedEvent.getPaymentId()).isEqualTo(paymentEvent.getPaymentId());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                wiremock.verify(2, postRequestedFor(urlEqualTo("/subscription"))
                        .withRequestBody(equalToJson(jsonBody()))
        ));

        awaitCommittedOffsetsAtLogEnd(enrichedConsumerGroupId);
    }

    @Test
    public void shouldRetryWhenCustomerNotFoundAndEventuallySucceed() {
        stubHttpResponses(List.of(ResponseStub.SUCCESS));

        Long nonExistentCustomerId = 999L;
        PaymentEvent paymentEvent = createPaymentEventWithCustomerId(nonExistentCustomerId);

        kafkaTemplate.send(paymentEventsTopic, paymentEvent);

        // Wait a bit to allow retries to happen
        await().pollDelay(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2)).until(() -> true);

        // Verify no enriched event was produced (customer not found)
        assertThat(enrichedPaymentEventsConsumerHelper.getReceivedEvents()).isEmpty();

        // add the customer to the database
        Customer customer = new Customer();
        customer.setId(nonExistentCustomerId);
        customer.setEmail("retry.customer@example.com");
        customer.setName("Retry Customer");
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        customerRepository.save(customer);

        // The retry mechanism should eventually succeed and produce the enriched event
        boolean messageReceived = enrichedPaymentEventsConsumerHelper.awaitEvent(10);
        assertThat(messageReceived).isTrue();
        assertThat(enrichedPaymentEventsConsumerHelper.getReceivedEvents()).hasSize(1);

        EnrichedPaymentEvent receivedEvent = enrichedPaymentEventsConsumerHelper.getReceivedEvents().getFirst();
        assertThat(receivedEvent.getPaymentId()).isEqualTo(paymentEvent.getPaymentId());
        assertThat(receivedEvent.getCustomer().getId()).isEqualTo(nonExistentCustomerId);
        assertThat(receivedEvent.getCustomer().getEmail()).isEqualTo("retry.customer@example.com");
        assertThat(receivedEvent.getCustomer().getName()).isEqualTo("Retry Customer");

        // Verify webhook was called
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                wiremock.verify(1, postRequestedFor(urlEqualTo(SUBSCRIPTION)))
        );
    }

    private void compareEvents(EnrichedPaymentEvent receivedEvent, PaymentEvent paymentEvent) {
        assertThat(receivedEvent.getPaymentId()).isEqualTo(paymentEvent.getPaymentId());
        assertThat(receivedEvent.getIdempotencyKey()).isEqualTo(paymentEvent.getIdempotencyKey());
        assertThat(receivedEvent.getPaymentData().getCreatedAt()).isEqualTo(paymentEvent.getPaymentData().getCreatedAt());
        assertThat(receivedEvent.getPaymentData().getAmount()).isEqualTo(paymentEvent.getPaymentData().getAmount());
        assertThat(receivedEvent.getPaymentData().getCurrency()).isEqualTo(paymentEvent.getPaymentData().getCurrency());
        assertThat(receivedEvent.getPaymentData().getPaymentStatus()).isEqualTo(paymentEvent.getPaymentData().getPaymentStatus());
        assertThat(receivedEvent.getCustomerId()).isEqualTo(paymentEvent.getCustomerId());
        assertThat(receivedEvent.getMerchantId()).isEqualTo(paymentEvent.getMerchantId());
        assertThat(receivedEvent.getCustomer().getId()).isEqualTo(savedCustomerId);
        assertThat(receivedEvent.getCustomer().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(receivedEvent.getCustomer().getName()).isEqualTo("John Doe");
    }


    private String jsonBody() {
        return """
                {
                  "paymentId": "pay_123",
                  "idempotencyKey": "pay_123-2025-02-22T10:15:30Z",
                  "customerId": "123",
                  "merchantId": "456",
                  "paymentData": {
                    "amount": "10000",
                    "currency": "USD",
                    "paymentStatus": "PAYMENT_COMPLETED",
                    "createdAt": "2025-03-17T23:00:00Z"
                  },
                  "customer": {
                    "id": "123",
                    "email": "john.doe@example.com",
                    "name": "John Doe"
                  }
                }
        """;
    }

    record ResponseStub(int statusCode, String responseBody) {
        public static final String SUCCESS_BODY = "{\"status\":\"success\"}";
        public static final String FAILURE_BODY = "{\"status\":\"error\"}";

        public static ResponseStub SUCCESS = new ResponseStub(200, SUCCESS_BODY);
        public static ResponseStub ERROR = new ResponseStub(500, FAILURE_BODY);
    }

    private void stubHttpResponsesWithTimeouts() {
        var scenario = "test-timeout";
        var postTimeout = "post-timeout";
        wiremock.stubFor(post(urlEqualTo(SUBSCRIPTION)).willReturn(
                aResponse()
                        .withStatus(200)
                        .withFixedDelay(timeout + 10000))
                .inScenario(scenario)
                .willSetStateTo(postTimeout)
                .whenScenarioStateIs(Scenario.STARTED));
        wiremock.stubFor(post(urlEqualTo(SUBSCRIPTION)).inScenario(scenario)
                .whenScenarioStateIs(postTimeout)
                .willReturn(getResponseDefinitionBuilder(ResponseStub.SUCCESS))
                .willSetStateTo("success"));
    }

    private void stubHttpResponses(List<ResponseStub> responseStubList) {
        var scenario = "test";
        var state = Scenario.STARTED;
        var counter = 0;
        for (ResponseStub responseStub : responseStubList) {
            var nextState = "state " + (counter++);
            wiremock.stubFor(post(urlEqualTo(SUBSCRIPTION)).inScenario(scenario)
                            .whenScenarioStateIs(state)
                    .withRequestBody(equalToJson(jsonBody()))
                    .willReturn(getResponseDefinitionBuilder(responseStub))
                    .willSetStateTo(nextState));
            state = nextState;
        }
    }

    private static ResponseDefinitionBuilder getResponseDefinitionBuilder(ResponseStub responseStub) {
        return aResponse()
                .withStatus(responseStub.statusCode)
                .withHeader("Content-Type", "application/json")
                .withBody(responseStub.responseBody);
    }

    private PaymentEvent createSamplePaymentEvent() {
        return createPaymentEventWithCustomerId(savedCustomerId);
    }

    private PaymentEvent createPaymentEventWithCustomerId(Long customerId) {
        return TestDataBuilder.createPaymentEvent(customerId, savedMerchantId);
    }

    private void awaitCommittedOffsetsAtLogEnd(String groupId) {
        System.out.println("Awaiting committed offsets at " + groupId);
        try (var admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
             var probe = new KafkaConsumer<>(
                 getConsumerProps(kafka.getBootstrapServers()),
                 new LongDeserializer(),
                 new ByteArrayDeserializer())) {

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var committed = admin.listConsumerGroupOffsets(groupId)
                                     .partitionsToOffsetAndMetadata().get();
                
                // Skip if no offsets are committed yet
                assertThat(committed).isNotEmpty();
                
                // If topic has multiple partitions, we compare all of them
                var tps = committed.keySet();
                var endOffsets = probe.endOffsets(tps);

                for (var tp : tps) {
                    long committedOffset = committed.get(tp).offset();
                    long end = endOffsets.get(tp);
                    assertThat(committedOffset).isEqualTo(end);
                }
            });
        } catch (Exception e) {
            log.error("Error verifying committed offsets", e);
            throw new RuntimeException("Failed to verify offsets", e);
        }
    }
    
    private Map<String, Object> getConsumerProps(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "offset-verifier");
        props.put("auto.offset.reset", "earliest");
        return props;
    }
}
