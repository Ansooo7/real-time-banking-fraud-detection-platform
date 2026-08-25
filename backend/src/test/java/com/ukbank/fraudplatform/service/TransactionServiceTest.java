package com.ukbank.fraudplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukbank.fraudplatform.dto.MLPredictResponse;
import com.ukbank.fraudplatform.dto.TransactionRequest;
import com.ukbank.fraudplatform.dto.TransactionResponse;
import com.ukbank.fraudplatform.exception.InsufficientFundsException;
import com.ukbank.fraudplatform.model.*;
import com.ukbank.fraudplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private FraudAlertRepository fraudAlertRepository;
    @Mock private RiskProfileRepository riskProfileRepository;
    @Mock private RuleEngineService ruleEngineService;
    @Mock private MLClientService mlClientService;
    @Mock private CompositeRiskEvaluator compositeRiskEvaluator;
    @Mock private IdempotencyService idempotencyService;
    @Mock private AuditService auditService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionService transactionService;

    private Customer testCustomer;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .customerNumber("CUST-101")
                .firstName("Oliver")
                .lastName("Twist")
                .email("oliver@twist.co.uk")
                .phoneNumber("+447123456789")
                .build();

        testAccount = Account.builder()
                .id(UUID.randomUUID())
                .customer(testCustomer)
                .accountNumber("12345678")
                .sortCode("204514")
                .balance(new BigDecimal("1000.00"))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Should approve low-risk transaction and deduct account balance")
    void testProcessApprovedTransaction() {
        TransactionRequest request = TransactionRequest.builder()
                .sourceAccountId(testAccount.getId())
                .destinationAccountNumber("87654321")
                .amount(new BigDecimal("150.00"))
                .channel(Channel.MOBILE_APP)
                .build();

        when(idempotencyService.getCachedResponse(any())).thenReturn(Optional.empty());
        when(idempotencyService.isDuplicate(any())).thenReturn(false);
        when(accountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));
        when(riskProfileRepository.findById(testCustomer.getId())).thenReturn(Optional.empty());
        when(riskProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(ruleEngineService.evaluate(any(), any(), any(), anyBoolean()))
                .thenReturn(RuleEngineService.RuleEvaluationResult.builder()
                        .ruleScore(5)
                        .triggeredRules(List.of())
                        .build());

        when(mlClientService.predictRisk(any()))
                .thenReturn(MLPredictResponse.builder()
                        .riskScore(10)
                        .fraudProbability(0.10)
                        .riskFactors(Map.of())
                        .build());

        when(compositeRiskEvaluator.evaluate(anyInt(), anyInt(), anyBoolean(), anyList()))
                .thenReturn(CompositeRiskEvaluator.CompositeEvaluation.builder()
                        .ruleScore(5)
                        .mlScore(10)
                        .compositeRiskScore(8)
                        .decision(TransactionStatus.APPROVED)
                        .decisionReason("Low risk")
                        .build());

        when(transactionRepository.save(any())).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        TransactionResponse response = transactionService.processTransaction(request, "idemp-key-123");

        assertNotNull(response);
        assertEquals(TransactionStatus.APPROVED, response.getStatus());
        assertEquals(8, response.getRiskScore());
        assertEquals(new BigDecimal("850.00"), testAccount.getBalance()); // Balance deducted

        verify(accountRepository, times(1)).save(testAccount);
        verify(idempotencyService, times(1)).saveKey(eq("idemp-key-123"), any());
        verify(auditService, times(1)).logAction(eq("TRANSACTION"), any(), eq("PROCESSED"), isNull(), any());
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when balance is less than transaction amount")
    void testInsufficientFundsException() {
        TransactionRequest request = TransactionRequest.builder()
                .sourceAccountId(testAccount.getId())
                .destinationAccountNumber("87654321")
                .amount(new BigDecimal("2500.00")) // Exceeds balance of £1000
                .channel(Channel.ONLINE_BANKING)
                .build();

        when(idempotencyService.getCachedResponse(any())).thenReturn(Optional.empty());
        when(idempotencyService.isDuplicate(any())).thenReturn(false);
        when(accountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));

        assertThrows(InsufficientFundsException.class, () -> {
            transactionService.processTransaction(request, "idemp-key-456");
        });

        verify(transactionRepository, never()).save(any());
    }
}
