package com.example.mandateguard;

import com.example.mandateguard.dto.CreatePaymentRequest;
import com.example.mandateguard.entity.AiAgent;
import com.example.mandateguard.entity.PaymentMandate;
import com.example.mandateguard.enums.AgentStatus;
import com.example.mandateguard.enums.DecisionReasonCode;
import com.example.mandateguard.enums.MandateStatus;
import com.example.mandateguard.enums.PaymentDecision;
import com.example.mandateguard.service.MandateEvaluationService;
import com.example.mandateguard.service.PaymentEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MandateEvaluationServiceTest {

    private MandateEvaluationService evaluationService;
    private AiAgent agent;
    private PaymentMandate mandate;

    @BeforeEach
    void setUp() {
        evaluationService = new MandateEvaluationService();

        agent = AiAgent.builder()
                .id(UUID.randomUUID())
                .status(AgentStatus.ACTIVE)
                .build();

        mandate = PaymentMandate.builder()
                .id(UUID.randomUUID())
                .reference("MND-TEST-001")
                .name("Test spending mandate")
                .status(MandateStatus.ACTIVE)
                .currencyCode("USD")
                .maximumSingleAmount(
                        new BigDecimal("1000.00")
                )
                .totalBudget(new BigDecimal("5000.00"))
                .consumedAmount(new BigDecimal("0.00"))
                .approvalThreshold(
                        new BigDecimal("500.00")
                )
                .maximumTransactions(10)
                .usedTransactionCount(0)
                .allowAnyVerifiedMerchant(false)
                .allowedCountryCodes(Set.of("US"))
                .allowedMerchantIdentifiers(
                        Set.of("MERCHANT-001")
                )
                .allowedMerchantCategoryCodes(
                        Set.of("3000")
                )
                .agent(agent)
                .validFrom(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void shouldAllowPaymentWhenAllRulesPass() {
        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.ALLOWED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.ALL_RULES_PASSED
                );
    }

    @Test
    void shouldRequireApprovalWhenAmountExceedsThreshold() {
        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("750.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(
                        PaymentDecision.REQUIRES_APPROVAL
                );

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.HUMAN_APPROVAL_REQUIRED
                );
    }
    @Test
    void shouldDenyPaymentExceedingSingleAmountLimit() {
        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("1200.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode
                                .SINGLE_AMOUNT_LIMIT_EXCEEDED
                );
    }

    @Test
    void shouldDenyPaymentUsingDifferentCurrency() {
        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "EUR",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.CURRENCY_NOT_ALLOWED
                );
    }

    @Test
    void shouldDenyPaymentFromDisallowedCountry() {
        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "GB"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.COUNTRY_NOT_ALLOWED
                );
    }

    @Test
    void shouldDenyPaymentToDisallowedMerchant() {
        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "UNKNOWN-MERCHANT",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.MERCHANT_NOT_ALLOWED
                );
    }

    @Test
    void shouldDenyDisallowedMerchantCategory() {
        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "MERCHANT-001",
                        "7995",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode
                                .MERCHANT_CATEGORY_NOT_ALLOWED
                );
    }

    @Test
    void shouldDenyPaymentExceedingRemainingBudget() {
        mandate.setConsumedAmount(
                new BigDecimal("4800.00")
        );

        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.TOTAL_BUDGET_EXCEEDED
                );
    }

    @Test
    void shouldDenyPaymentWhenTransactionLimitReached() {
        mandate.setUsedTransactionCount(10);

        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode
                                .TRANSACTION_LIMIT_EXCEEDED
                );
    }

    @Test
    void shouldDenyPaymentWhenMandateIsSuspended() {
        mandate.setStatus(MandateStatus.SUSPENDED);

        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.MANDATE_NOT_ACTIVE
                );
    }

    @Test
    void shouldDenyPaymentWhenMandateHasExpired() {
        mandate.setExpiresAt(
                Instant.now().minusSeconds(60)
        );

        CreatePaymentRequest request =
                createRequest(
                        new BigDecimal("250.00"),
                        "USD",
                        "MERCHANT-001",
                        "3000",
                        "US"
                );

        PaymentEvaluationResult result =
                evaluationService.evaluate(
                        agent,
                        mandate,
                        request
                );

        assertThat(result.decision())
                .isEqualTo(PaymentDecision.DENIED);

        assertThat(result.reasonCode())
                .isEqualTo(
                        DecisionReasonCode.MANDATE_EXPIRED
                );
    }

    private CreatePaymentRequest createRequest(
            BigDecimal amount,
            String currencyCode,
            String merchantIdentifier,
            String categoryCode,
            String countryCode
    ) {
        return new CreatePaymentRequest(
                mandate.getId(),
                "PAY-" + UUID.randomUUID(),
                amount,
                currencyCode,
                merchantIdentifier,
                "Test Merchant",
                categoryCode,
                countryCode,
                true,
                "Automated test payment",
                Instant.now()
        );
    }
}