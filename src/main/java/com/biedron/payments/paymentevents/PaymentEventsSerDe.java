package com.biedron.payments.paymentevents;

import com.biedron.payments.schema.v1.PaymentEvent;
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
public class PaymentEventsSerDe implements Deserializer<PaymentEvent>, Serializer<PaymentEvent>, Serde<PaymentEvent> {

    @Override
    public PaymentEvent deserialize(String topic, byte[] message) {
        try {
            return PaymentEvent.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing PaymentEvent message",
                    kv("exception", e.getMessage()),
                    kv("stacktrace", e.getStackTrace()));
            return null;
        }
    }

    @Override
    public byte[] serialize(String topic, PaymentEvent PaymentEvent) {
        return PaymentEvent.toByteArray();
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
    public Serializer<PaymentEvent> serializer() {
        return this;
    }

    @Override
    public Deserializer<PaymentEvent> deserializer() {
        return this;
    }
}

