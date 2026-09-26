package com.example.mandateguard.service;

import com.example.mandateguard.dto.CreateMandateRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

@Service
public class MandateValidationService {

    private static final Set<String> ISO_COUNTRY_CODES =
            Set.of(Locale.getISOCountries());

    public void validate(CreateMandateRequest request) {
        validateAmounts(request);
        validateDates(request);
        validateCurrency(request.currencyCode());
        validateCountries(request.allowedCountryCodes());
        validateMerchantCategories(request.allowedMerchantCategoryCodes());
        validateMerchantRules(request);
    }

    private void validateAmounts(CreateMandateRequest request) {
        if (request.maxSingleAmount()
                .compareTo(request.totalBudget()) > 0) {
            throw new IllegalArgumentException(
                    "Maximum single amount cannot exceed total budget"
            );
        }

        if (request.approvalThreshold() != null &&
                request.approvalThreshold()
                        .compareTo(request.maxSingleAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Approval threshold cannot exceed maximum single amount"
            );
        }
    }

    private void validateDates(CreateMandateRequest request) {
        if (!request.validFrom().isBefore(request.expiresAt())) {
            throw new IllegalArgumentException(
                    "Valid-from time must be before expiration time"
            );
        }

        if (!request.expiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Mandate expiration time must be in the future"
            );
        }
    }

    private void validateCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid ISO currency code: " + currencyCode
            );
        }
    }

    private void validateCountries(Set<String> countryCodes) {
        if (countryCodes == null) {
            return;
        }

        for (String countryCode : countryCodes) {
            if (countryCode == null ||
                    !ISO_COUNTRY_CODES.contains(countryCode.toUpperCase())) {
                throw new IllegalArgumentException(
                        "Invalid ISO country code: " + countryCode
                );
            }
        }
    }

    private void validateMerchantCategories(Set<String> categoryCodes) {
        if (categoryCodes == null) {
            return;
        }

        for (String categoryCode : categoryCodes) {
            if (categoryCode == null ||
                    !categoryCode.matches("\\d{4}")) {
                throw new IllegalArgumentException(
                        "Merchant category code must contain exactly 4 digits"
                );
            }
        }
    }

    private void validateMerchantRules(CreateMandateRequest request) {
        boolean hasMerchantIds =
                request.allowedMerchantIds() != null &&
                        !request.allowedMerchantIds().isEmpty();

        boolean hasCategoryCodes =
                request.allowedMerchantCategoryCodes() != null &&
                        !request.allowedMerchantCategoryCodes().isEmpty();

        if (!request.allowAnyVerifiedMerchant() &&
                !hasMerchantIds &&
                !hasCategoryCodes) {
            throw new IllegalArgumentException(
                    "Provide an allowed merchant or category when unrestricted merchants are disabled"
            );
        }
    }
}