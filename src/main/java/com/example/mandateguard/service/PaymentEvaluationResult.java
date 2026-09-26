package com.example.mandateguard.service;

import com.example.mandateguard.enums.DecisionReasonCode;
import com.example.mandateguard.enums.PaymentDecision;
import com.example.mandateguard.enums.PaymentRequestStatus;

public record PaymentEvaluationResult(

        PaymentDecision decision,
        PaymentRequestStatus status,
        DecisionReasonCode reasonCode,
        String explanation
) {
}