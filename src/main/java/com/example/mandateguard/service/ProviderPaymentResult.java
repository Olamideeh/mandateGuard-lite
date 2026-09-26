package com.example.mandateguard.service;

public record ProviderPaymentResult(

        boolean successful,
        String providerReference,
        String message
) {
}