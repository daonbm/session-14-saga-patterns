package com.session14.saga.common;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, SagaEvent> sagaProducerFactory(@Value("${spring.kafka.bootstrap-servers}") String servers) {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"
        ));
    }

    @Bean
    public KafkaTemplate<String, SagaEvent> sagaKafkaTemplate(ProducerFactory<String, SagaEvent> sagaProducerFactory) {
        return new KafkaTemplate<>(sagaProducerFactory);
    }
}