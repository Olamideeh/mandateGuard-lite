package com.example.mandateguard.service;

import com.example.mandateguard.dto.CreatePaymentRequest;
import com.example.mandateguard.entity.AiAgent;
import com.example.mandateguard.entity.PaymentMandate;
import com.example.mandateguard.enums.DecisionReasonCode;
import com.example.mandateguard.enums.MandateStatus;
import com.example.mandateguard.enums.PaymentDecision;
import com.example.mandateguard.enums.PaymentRequestStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MandateEvaluationService {

    public PaymentEvaluationResult evaluate(
            AiAgent agent,
            PaymentMandate mandate,
            CreatePaymentRequest request
    ) {
        if (mandate.getStatus() == MandateStatus.REVOKED) {
            return denied(
                    DecisionReasonCode.MANDATE_REVOKED,
                    "The mandate has been permanently revoked"
            );
        }

        if (mandate.getStatus() == MandateStatus.EXPIRED ||
                !mandate.getExpiresAt().isAfter(Instant.now())) {
            return denied(
                    DecisionReasonCode.MANDATE_EXPIRED,
                    "The mandate has expired"
            );
        }

        if (mandate.getStatus() == MandateStatus.EXHAUSTED) {
            return denied(
                    DecisionReasonCode.MANDATE_EXHAUSTED,
                    "The mandate has been exhausted"
            );
        }

        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            return denied(
                    DecisionReasonCode.MANDATE_NOT_ACTIVE,
                    "The mandate is not active"
            );
        }

        if (mandate.getValidFrom().isAfter(Instant.now())) {
            return denied(
                    DecisionReasonCode.MANDATE_NOT_ACTIVE,
                    "The mandate validity period has not started"
            );
        }

        if (!mandate.getAgent().getId().equals(agent.getId())) {
            return denied(
                    DecisionReasonCode.AGENT_NOT_AUTHORIZED,
                    "The AI agent is not authorized by this mandate"
            );
        }

        if (!mandate.getCurrencyCode().equalsIgnoreCase(
                request.currencyCode()
        )) {
            return denied(
                    DecisionReasonCode.CURRENCY_NOT_ALLOWED,
                    "The requested currency is not allowed"
            );
        }

        if (!mandate.getAllowedCountryCodes().isEmpty() &&
                !mandate.getAllowedCountryCodes().contains(
                        request.merchantCountryCode().toUpperCase()
                )) {
            return denied(
                    DecisionReasonCode.COUNTRY_NOT_ALLOWED,
                    "The merchant country is not allowed"
            );
        }

        if (mandate.isAllowAnyVerifiedMerchant()) {
            if (!request.merchantVerified()) {
                return denied(
                        DecisionReasonCode.MERCHANT_NOT_ALLOWED,
                        "The merchant must be verified"
                );
            }
        } else if (!mandate.getAllowedMerchantIdentifiers().isEmpty() &&
                !mandate.getAllowedMerchantIdentifiers().contains(
                        request.merchantIdentifier()
                )) {
            return denied(
                    DecisionReasonCode.MERCHANT_NOT_ALLOWED,
                    "The merchant is not included in the mandate"
            );
        }

        if (!mandate.getAllowedMerchantCategoryCodes().isEmpty() &&
                !mandate.getAllowedMerchantCategoryCodes().contains(
                        request.merchantCategoryCode()
                )) {
            return denied(
                    DecisionReasonCode.MERCHANT_CATEGORY_NOT_ALLOWED,
                    "The merchant category is not allowed"
            );
        }

        if (request.amount().compareTo(
                mandate.getMaximumSingleAmount()
        ) > 0) {
            return denied(
                    DecisionReasonCode.SINGLE_AMOUNT_LIMIT_EXCEEDED,
                    "The amount exceeds the single-payment limit"
            );
        }

        if (mandate.getConsumedAmount()
                .add(request.amount())
                .compareTo(mandate.getTotalBudget()) > 0) {
            return denied(
                    DecisionReasonCode.TOTAL_BUDGET_EXCEEDED,
                    "The payment would exceed the total mandate budget"
            );
        }

        if (mandate.getUsedTransactionCount() >=
                mandate.getMaximumTransactions()) {
            return denied(
                    DecisionReasonCode.TRANSACTION_LIMIT_EXCEEDED,
                    "The mandate transaction limit has been reached"
            );
        }

        if (mandate.getApprovalThreshold() != null &&
                request.amount().compareTo(
                        mandate.getApprovalThreshold()
                ) > 0) {
            return new PaymentEvaluationResult(
                    PaymentDecision.REQUIRES_APPROVAL,
                    PaymentRequestStatus.AWAITING_APPROVAL,
                    DecisionReasonCode.HUMAN_APPROVAL_REQUIRED,
                    "The payment requires approval from the principal"
            );
        }

        return new PaymentEvaluationResult(
                PaymentDecision.ALLOWED,
                PaymentRequestStatus.AUTHORIZED,
                DecisionReasonCode.ALL_RULES_PASSED,
                "The payment satisfies all mandate rules"
        );
    }

    private PaymentEvaluationResult denied(
            DecisionReasonCode reasonCode,
            String explanation
    ) {
        return new PaymentEvaluationResult(
                PaymentDecision.DENIED,
                PaymentRequestStatus.REJECTED,
                reasonCode,
                explanation
        );
    }
}