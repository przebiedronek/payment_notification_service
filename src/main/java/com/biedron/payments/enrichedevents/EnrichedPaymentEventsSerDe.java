package com.biedron.payments.enrichedevents;

import com.biedron.payments.schema.v1.EnrichedPaymentEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.stereotype.Component;

import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
public class EnrichedPaymentEventsSerDe implements Deserializer<EnrichedPaymentEvent>, Serializer<EnrichedPaymentEvent>, Serde<EnrichedPaymentEvent> {

    @Override
    public EnrichedPaymentEvent deserialize(String topic, byte[] message) {
        try {
            return EnrichedPaymentEvent.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing EnrichedPaymentEvent message",
                    kv("exception", e.getMessage()),
                    kv("stacktrace", e.getStackTrace()));
            return null;
        }
    }

    @Override
    public byte[] serialize(String topic, EnrichedPaymentEvent EnrichedPaymentEvent) {
        return EnrichedPaymentEvent.toByteArray();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }

    @Override
    public Serializer<EnrichedPaymentEvent> serializer() {
        return this;
    }

    @Override
    public Deserializer<EnrichedPaymentEvent> deserializer() {
        return this;
    }
}

