package com.ukbank.fraudplatform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    public static final String TOPIC_TRANSACTIONS_CREATED = "bank.transactions.created";
    public static final String TOPIC_FRAUD_EVALUATED = "bank.fraud.evaluated";
    public static final String TOPIC_FRAUD_ALERTS = "bank.fraud.alerts";

    @Bean
    public NewTopic transactionsCreatedTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTIONS_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudEvaluatedTopic() {
        return TopicBuilder.name(TOPIC_FRAUD_EVALUATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name(TOPIC_FRAUD_ALERTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
