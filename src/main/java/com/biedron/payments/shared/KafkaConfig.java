package com.biedron.payments.shared;

import com.biedron.payments.enrichedevents.EnrichedPaymentEventsSerDe;
import com.biedron.payments.paymentevents.PaymentEventsSerDe;
import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.BackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${KAFKA_PAYMENT_CONSUMER_GROUP}")
    private String paymentEventsConsumerGroup;

    @Value("${KAFKA_ENRICHED_CONSUMER_GROUP}")
    private String enrichedEventsConsumerGroup;

    @Value("${spring.application.app.rest.blocking.retry.maxAttempts}")
    private int retryMaxAttempts;

    @Value("${spring.application.app.rest.blocking.retry.delay}")
    private int delay;

    @Value("${spring.application.app.rest.blocking.retry.multiplier}")
    private int multiplier;

    @Bean("paymentEventsConsumerFactory")
    public ConsumerFactory<String, byte[]> paymentEventsConsumerFactory(
            KafkaProperties kafkaProperties,
            SslBundles sslBundles) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties(sslBundles));
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PaymentEventsSerDe.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, paymentEventsConsumerGroup);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> paymentEventsKafkaListenerContainerFactory(
            @Qualifier("paymentEventsConsumerFactory") ConsumerFactory<String, byte[]> consumerFactory, BackOff backOff) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, byte[]>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(backOff));
        return factory;
    }

    @Bean
    public BackOff backOffPolicy() {
        var exponentialBackOffWithMaxRetries = new ExponentialBackOffWithMaxRetries(retryMaxAttempts);
        exponentialBackOffWithMaxRetries.setInitialInterval(delay);
        exponentialBackOffWithMaxRetries.setMultiplier(multiplier);
        return exponentialBackOffWithMaxRetries;
    }

    @Bean("enrichedPaymentEventsConsumerFactory")
    public ConsumerFactory<Long, byte[]> enrichedPaymentEventsConsumerFactory(
            KafkaProperties kafkaProperties,
            SslBundles sslBundles) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties(sslBundles));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EnrichedPaymentEventsSerDe.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, enrichedEventsConsumerGroup);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, byte[]> enrichedPaymentEventsKafkaListenerContainerFactory(
            @Qualifier("enrichedPaymentEventsConsumerFactory")  ConsumerFactory<Long, byte[]> consumerFactory, BackOff backOff) {
        var factory = new ConcurrentKafkaListenerContainerFactory<Long, byte[]>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(backOff));
        return factory;
    }

    @Bean
    public ProducerFactory<Long, EnrichedPaymentEvent> producerFactory(
            KafkaProperties kafkaProperties,
            SslBundles sslBundles) {
        var config = new HashMap<>(kafkaProperties.buildProducerProperties(sslBundles));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        // Enable idempotent producer for exactly-once semantics
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<Long, EnrichedPaymentEvent> kafkaTemplate(ProducerFactory<Long, EnrichedPaymentEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}

