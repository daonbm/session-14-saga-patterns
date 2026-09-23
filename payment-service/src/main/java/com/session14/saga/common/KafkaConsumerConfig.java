package com.session14.saga.common;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, SagaEvent> sagaConsumerFactory(@Value("${spring.kafka.bootstrap-servers}") String servers) {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers,
                ConsumerConfig.GROUP_ID_CONFIG, "saga-consumer",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.session14.saga.common"
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SagaEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, SagaEvent> sagaConsumerFactory,
            CommonErrorHandler commonErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, SagaEvent>();
        factory.setConsumerFactory(sagaConsumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public CommonErrorHandler commonErrorHandler() {
        return new DefaultErrorHandler((record, ex) ->
                System.err.println("Kafka error on " + record.topic() + ": " + ex.getMessage()));
    }
}