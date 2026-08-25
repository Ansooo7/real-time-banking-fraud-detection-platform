package com.ukbank.fraudplatform.service;

import com.ukbank.fraudplatform.event.FraudEvaluatedEvent;
import com.ukbank.fraudplatform.event.TransactionCreatedEvent;
import com.ukbank.fraudplatform.model.Transaction;
import com.ukbank.fraudplatform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

import static com.ukbank.fraudplatform.config.KafkaConfig.TOPIC_FRAUD_EVALUATED;
import static com.ukbank.fraudplatform.config.KafkaConfig.TOPIC_TRANSACTIONS_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class FraudDetectionConsumer {

    private final TransactionRepository transactionRepository;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = TOPIC_TRANSACTIONS_CREATED, groupId = "fraud-detection-service-group")
    public void consumeTransactionCreatedEvent(TransactionCreatedEvent event) {
        log.info("Kafka consumer received TransactionCreatedEvent [EventID: {}] for tx: {}", 
                event.getEventId(), event.getTransactionId());

        Transaction tx = transactionRepository.findById(event.getTransactionId()).orElse(null);
        if (tx == null) {
            log.warn("Transaction {} not found in database for Kafka event.", event.getTransactionId());
            return;
        }

        // Publish evaluated summary to bank.fraud.evaluated topic
        if (kafkaTemplate != null) {
            try {
                FraudEvaluatedEvent evaluatedEvent = FraudEvaluatedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .transactionId(tx.getId())
                        .customerId(event.getCustomerId())
                        .ruleScore(tx.getRiskScore() != null ? tx.getRiskScore() : 0)
                        .mlScore(tx.getRiskScore() != null ? tx.getRiskScore() : 0)
                        .compositeRiskScore(tx.getRiskScore() != null ? tx.getRiskScore() : 0)
                        .decision(tx.getStatus())
                        .timestamp(ZonedDateTime.now())
                        .build();

                kafkaTemplate.send(TOPIC_FRAUD_EVALUATED, event.getCustomerId().toString(), evaluatedEvent);
                log.debug("Dispatched FraudEvaluatedEvent to topic: {}", TOPIC_FRAUD_EVALUATED);
            } catch (Exception e) {
                log.error("Failed to emit FraudEvaluatedEvent for tx {}: {}", tx.getId(), e.getMessage());
            }
        }
    }
}
