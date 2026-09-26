package com.example.mandateguard.service;

import com.example.mandateguard.entity.AgentPaymentRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SimulatedPaymentProvider {

    public ProviderPaymentResult process(
            AgentPaymentRequest paymentRequest
    ) {
        boolean simulateDecline =
                paymentRequest.getMerchantIdentifier()
                        .toUpperCase()
                        .contains("DECLINE")
                        ||
                        (
                                paymentRequest.getDescription() != null &&
                                        paymentRequest.getDescription()
                                                .toUpperCase()
                                                .contains("SIMULATE_DECLINE")
                        );

        if (simulateDecline) {
            return new ProviderPaymentResult(
                    false,
                    null,
                    "The simulated provider declined the payment"
            );
        }

        String providerReference =
                "SIM-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();

        return new ProviderPaymentResult(
                true,
                providerReference,
                "The simulated provider processed the payment"
        );
    }
}
