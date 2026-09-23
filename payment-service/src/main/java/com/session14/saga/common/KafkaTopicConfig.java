package com.session14.saga.common;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic orderEvents() {
        return new NewTopic(Topics.ORDER_EVENTS, 1, (short) 1);
    }

    @Bean
    NewTopic paymentEvents() {
        return new NewTopic(Topics.PAYMENT_EVENTS, 1, (short) 1);
    }

    @Bean
    NewTopic inventoryEvents() {
        return new NewTopic(Topics.INVENTORY_EVENTS, 1, (short) 1);
    }
}