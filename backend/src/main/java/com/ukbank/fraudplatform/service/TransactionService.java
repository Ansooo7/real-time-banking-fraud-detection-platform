package com.ukbank.fraudplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukbank.fraudplatform.dto.*;
import com.ukbank.fraudplatform.event.TransactionCreatedEvent;
import com.ukbank.fraudplatform.exception.DuplicateTransactionException;
import com.ukbank.fraudplatform.exception.InsufficientFundsException;
import com.ukbank.fraudplatform.exception.ResourceNotFoundException;
import com.ukbank.fraudplatform.model.*;
import com.ukbank.fraudplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.ukbank.fraudplatform.config.KafkaConfig.TOPIC_TRANSACTIONS_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final DeviceRepository deviceRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final RiskProfileRepository riskProfileRepository;
    
    private final RuleEngineService ruleEngineService;
    private final MLClientService mlClientService;
    private final CompositeRiskEvaluator compositeRiskEvaluator;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request, String idempotencyKey) {
        log.info("Processing transaction of £{} from account {}", request.getAmount(), request.getSourceAccountId());

        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<TransactionResponse> cached = idempotencyService.getCachedResponse(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Returning cached idempotent response for key: {}", idempotencyKey);
                return cached.get();
            }
            if (idempotencyService.isDuplicate(idempotencyKey)) {
                throw new DuplicateTransactionException("A transaction with idempotency key '" + idempotencyKey + "' has already been processed.");
            }
        } else {
            idempotencyKey = UUID.randomUUID().toString();
        }

        // 2. Validate Source Account & Balance
        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found with ID: " + request.getSourceAccountId()));

        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
            throw new InsufficientFundsException("Source account is not active (Status: " + sourceAccount.getStatus() + ")");
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Available: £" + sourceAccount.getBalance() + ", Requested: £" + request.getAmount());
        }

        Customer customer = sourceAccount.getCustomer();

        // 3. Resolve Merchant & Device
        Merchant merchant = null;
        if (request.getMerchantCode() != null && !request.getMerchantCode().isBlank()) {
            merchant = merchantRepository.findByMerchantCode(request.getMerchantCode()).orElse(null);
        }

        boolean isNewDevice = false;
        Device device = null;
        if (request.getDeviceFingerprint() != null && !request.getDeviceFingerprint().isBlank()) {
            Optional<Device> existingDevice = deviceRepository.findByCustomerIdAndDeviceFingerprint(
                    customer.getId(), request.getDeviceFingerprint());
            if (existingDevice.isPresent()) {
                device = existingDevice.get();
                device.setLastSeenAt(ZonedDateTime.now());
                if (request.getIpAddress() != null) device.setIpAddress(request.getIpAddress());
                deviceRepository.save(device);
                isNewDevice = Boolean.FALSE.equals(device.getIsTrusted());
            } else {
                isNewDevice = true;
                device = Device.builder()
                        .customer(customer)
                        .deviceFingerprint(request.getDeviceFingerprint())
                        .deviceType(request.getDeviceType() != null ? request.getDeviceType() : "UNKNOWN")
                        .ipAddress(request.getIpAddress() != null ? request.getIpAddress() : "127.0.0.1")
                        .locationCountry("GB")
                        .isTrusted(false)
                        .build();
                deviceRepository.save(device);
            }
        }

        // 4. Resolve Risk Profile
        RiskProfile profile = riskProfileRepository.findById(customer.getId())
                .orElseGet(() -> {
                    RiskProfile p = RiskProfile.builder()
                            .customerId(customer.getId())
                            .customer(customer)
                            .avgTransactionAmount30d(BigDecimal.valueOf(50.0))
                            .txCountLast24h(0)
                            .overallTrustScore(95)
                            .build();
                    return riskProfileRepository.save(p);
                });

        // 5. Initial Transaction Entity
        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .sourceAccount(sourceAccount)
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .merchant(merchant)
                .device(device)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .channel(request.getChannel())
                .status(TransactionStatus.PENDING)
                .ipAddress(request.getIpAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        // 6. Real-Time Hybrid Risk Evaluation
        // A. Rule Engine Evaluation
        RuleEngineService.RuleEvaluationResult ruleResult = ruleEngineService.evaluate(
                transaction, profile, merchant, isNewDevice);

        // B. ML Service Inference Call
        double geoDist = 0.0;
        if (profile.getLastKnownLatitude() != null && request.getLatitude() != null) {
            geoDist = calculateDistance(profile.getLastKnownLatitude(), profile.getLastKnownLongitude(),
                    request.getLatitude(), request.getLongitude());
        }

        MLPredictRequest mlRequest = MLPredictRequest.builder()
                .transactionId(transaction.getId() != null ? transaction.getId().toString() : UUID.randomUUID().toString())
                .customerId(customer.getId().toString())
                .amount(request.getAmount().doubleValue())
                .avgAmount30d(profile.getAvgTransactionAmount30d().doubleValue())
                .merchantRiskBase(merchant != null && merchant.getRiskScoreBase() != null ? merchant.getRiskScoreBase() : 10)
                .geoDistanceKm(geoDist)
                .txCount1h(Math.max(1, profile.getTxCountLast24h() / 4))
                .txCount24h(profile.getTxCountLast24h() + 1)
                .hourOfDay(ZonedDateTime.now().getHour())
                .isNewDevice(isNewDevice ? 1 : 0)
                .channel(request.getChannel().name())
                .build();

        MLPredictResponse mlResponse = mlClientService.predictRisk(mlRequest);

        // C. Composite Risk Scorer
        CompositeRiskEvaluator.CompositeEvaluation compositeEval = compositeRiskEvaluator.evaluate(
                ruleResult.getRuleScore(),
                mlResponse.getRiskScore(),
                ruleResult.isCriticalOverride(),
                ruleResult.getTriggeredRules()
        );

        // 7. Update Transaction Status & Balances
        transaction.setStatus(compositeEval.getDecision());
        transaction.setRiskScore(compositeEval.getCompositeRiskScore());
        transaction.setDecisionReason(compositeEval.getDecisionReason());

        if (compositeEval.getDecision() == TransactionStatus.APPROVED) {
            sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
            accountRepository.save(sourceAccount);
        }

        transaction = transactionRepository.save(transaction);

        // 8. Create Fraud Alert for REVIEW / BLOCKED Transactions
        if (compositeEval.getDecision() == TransactionStatus.REVIEW || 
            compositeEval.getDecision() == TransactionStatus.BLOCKED) {
            
            String triggersJson = "";
            String mlFactorsJson = "";
            try {
                triggersJson = objectMapper.writeValueAsString(ruleResult.getTriggeredRules());
                mlFactorsJson = objectMapper.writeValueAsString(mlResponse.getRiskFactors());
            } catch (Exception ignored) {}

            FraudAlert alert = FraudAlert.builder()
                    .transaction(transaction)
                    .customer(customer)
                    .ruleScore(ruleResult.getRuleScore())
                    .mlScore(mlResponse.getRiskScore())
                    .compositeRiskScore(compositeEval.getCompositeRiskScore())
                    .triggeredRules(triggersJson)
                    .mlFeatureContributions(mlFactorsJson)
                    .status(AlertStatus.PENDING_REVIEW)
                    .build();

            fraudAlertRepository.save(alert);
            log.warn("Created Fraud Alert [ID: {}] for transaction: {}", alert.getId(), transaction.getId());
        }

        // 9. Update Risk Profile
        profile.setTxCountLast24h(profile.getTxCountLast24h() + 1);
        profile.setLastTransactionTime(ZonedDateTime.now());
        if (request.getIpAddress() != null) profile.setLastKnownIp(request.getIpAddress());
        if (request.getLatitude() != null) profile.setLastKnownLatitude(request.getLatitude());
        if (request.getLongitude() != null) profile.setLastKnownLongitude(request.getLongitude());
        if (compositeEval.getDecision() == TransactionStatus.BLOCKED) {
            profile.setFraudIncidentCount(profile.getFraudIncidentCount() + 1);
            profile.setOverallTrustScore(Math.max(0, profile.getOverallTrustScore() - 25));
        }
        riskProfileRepository.save(profile);

        // 10. Produce Kafka Event
        if (kafkaTemplate != null) {
            try {
                TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("TRANSACTION_CREATED")
                        .correlationId(MDC.get("correlationId"))
                        .timestamp(ZonedDateTime.now())
                        .transactionId(transaction.getId())
                        .customerId(customer.getId())
                        .sourceAccountId(sourceAccount.getId())
                        .destinationAccountNumber(request.getDestinationAccountNumber())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .mcc(merchant != null ? merchant.getMcc() : "5999")
                        .merchantCode(merchant != null ? merchant.getMerchantCode() : null)
                        .channel(request.getChannel())
                        .deviceFingerprint(request.getDeviceFingerprint())
                        .ipAddress(request.getIpAddress())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .build();

                kafkaTemplate.send(TOPIC_TRANSACTIONS_CREATED, customer.getId().toString(), event);
                log.debug("Published transaction event to Kafka topic: {}", TOPIC_TRANSACTIONS_CREATED);
            } catch (Exception e) {
                log.error("Failed to publish Kafka event for transaction {}: {}", transaction.getId(), e.getMessage());
            }
        }

        // 11. Audit Log & Cache Response
        auditService.logAction("TRANSACTION", transaction.getId().toString(), "PROCESSED", null, transaction);

        TransactionResponse response = mapToResponse(transaction, customer);
        idempotencyService.saveKey(idempotencyKey, response);

        return response;
    }

    public TransactionResponse getTransactionById(UUID id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
        return mapToResponse(tx, tx.getSourceAccount().getCustomer());
    }

    public Page<TransactionResponse> getTransactions(TransactionStatus status, ZonedDateTime startDate, ZonedDateTime endDate, Pageable pageable) {
        return transactionRepository.findFiltered(status, startDate, endDate, pageable)
                .map(tx -> mapToResponse(tx, tx.getSourceAccount().getCustomer()));
    }

    public Page<TransactionResponse> getCustomerTransactions(UUID customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        return transactionRepository.findByCustomerId(customerId, pageable)
                .map(tx -> mapToResponse(tx, customer));
    }

    private TransactionResponse mapToResponse(Transaction tx, Customer customer) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .idempotencyKey(tx.getIdempotencyKey())
                .sourceAccountId(tx.getSourceAccount().getId())
                .sourceAccountNumber(tx.getSourceAccount().getAccountNumber())
                .customerName(customer != null ? customer.getFirstName() + " " + customer.getLastName() : "Unknown")
                .destinationAccountNumber(tx.getDestinationAccountNumber())
                .merchantName(tx.getMerchant() != null ? tx.getMerchant().getMerchantName() : "P2P Transfer")
                .merchantCategory(tx.getMerchant() != null ? tx.getMerchant().getCategoryName() : "Direct Transfer")
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .channel(tx.getChannel())
                .status(tx.getStatus())
                .riskScore(tx.getRiskScore())
                .decisionReason(tx.getDecisionReason())
                .ipAddress(tx.getIpAddress())
                .latitude(tx.getLatitude())
                .longitude(tx.getLongitude())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0;
    }
}
