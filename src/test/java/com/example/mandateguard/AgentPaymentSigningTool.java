package com.example.mandateguard;

import com.example.mandateguard.dto.CreatePaymentRequest;
import com.example.mandateguard.service.AgentSignatureService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class AgentPaymentSigningTool {

    public static void main(String[] args) throws Exception {

        UUID agentId = UUID.fromString(
                "PASTE_YOUR_AGENT_ID"
        );

        UUID mandateId = UUID.fromString(
                "PASTE_YOUR_MANDATE_ID"
        );

        String idempotencyKey =
                "payment-" + UUID.randomUUID();

        String nonce = UUID.randomUUID().toString();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        mandateId,
                        "PAY-" + UUID.randomUUID(),
                        new BigDecimal("250.00"),
                        "USD",
                        "GLOBAL-AIRLINE-001",
                        "Global Airline",
                        "3000",
                        "US",
                        true,
                        "Flight booking",
                        Instant.now()
                );

        AgentSignatureService signatureService =
                new AgentSignatureService();

        String canonicalPayload =
                signatureService.createCanonicalPayload(
                        agentId,
                        idempotencyKey,
                        nonce,
                        request
                );

        PrivateKey privateKey = readPrivateKey(
                Path.of(
                        "local-keys",
                        "agent-private-key.pem"
                )
        );

        Signature signer = Signature.getInstance(
                "SHA256withRSA"
        );

        signer.initSign(privateKey);
        signer.update(
                canonicalPayload.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );

        String signature = Base64.getEncoder()
                .encodeToString(signer.sign());

        System.out.println("X-Agent-Id: " + agentId);
        System.out.println(
                "Idempotency-Key: " + idempotencyKey
        );
        System.out.println("X-Nonce: " + nonce);
        System.out.println("X-Signature: " + signature);

        System.out.println();
        System.out.println("JSON BODY:");
        System.out.println("""
                {
                  "mandateId": "%s",
                  "paymentReference": "%s",
                  "amount": %s,
                  "currencyCode": "%s",
                  "merchantIdentifier": "%s",
                  "merchantName": "%s",
                  "merchantCategoryCode": "%s",
                  "merchantCountryCode": "%s",
                  "merchantVerified": %s,
                  "description": "%s",
                  "agentTimestamp": "%s"
                }
                """.formatted(
                request.mandateId(),
                request.paymentReference(),
                request.amount().toPlainString(),
                request.currencyCode(),
                request.merchantIdentifier(),
                request.merchantName(),
                request.merchantCategoryCode(),
                request.merchantCountryCode(),
                request.merchantVerified(),
                request.description(),
                request.agentTimestamp()
        ));
    }

    private static PrivateKey readPrivateKey(Path path)
            throws Exception {

        String privateKeyPem = Files.readString(path);

        String encodedKey = privateKeyPem
                .replace(
                        "-----BEGIN PRIVATE KEY-----",
                        ""
                )
                .replace(
                        "-----END PRIVATE KEY-----",
                        ""
                )
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder()
                .decode(encodedKey);

        PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(keyBytes);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(keySpec);
    }
}