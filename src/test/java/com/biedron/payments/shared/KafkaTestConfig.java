package com.biedron.payments.shared;

import com.biedron.payments.schema.v1.PaymentEvent;
import com.biedron.payments.paymentevents.PaymentEventsSerDe;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("test")
public class KafkaTestConfig {

    @Value("${spring.application.app.topics.payment-events}")
    private String paymentEventsTopic;

    @Value("${spring.application.app.topics.enriched-payment-events}")
    private String enrichedPaymentEventsTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(paymentEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic enrichedPaymentEventsTopic() {
        return TopicBuilder.name(enrichedPaymentEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean("paymentEventsProducerFactory")
    public ProducerFactory<String, PaymentEvent> producerFactory(
            KafkaProperties kafkaProperties,
            SslBundles sslBundles) {
        var config = new HashMap<>(kafkaProperties.buildProducerProperties(sslBundles));
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, PaymentEventsSerDe.class);
        // Enable idempotent producer for test environment
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean("paymentEventsKafkaTemplate")
    public KafkaTemplate<String, PaymentEvent> kafkaTemplate(@Qualifier("paymentEventsProducerFactory") ProducerFactory<String, PaymentEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}

