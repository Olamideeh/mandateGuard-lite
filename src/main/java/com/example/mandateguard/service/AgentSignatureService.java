package com.example.mandateguard.service;

import com.example.mandateguard.dto.CreatePaymentRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AgentSignatureService {

    public String verifyAndHash(
            UUID agentId,
            String idempotencyKey,
            String nonce,
            String signatureValue,
            String publicKeyPem,
            CreatePaymentRequest request
    ) {
        String canonicalPayload = createCanonicalPayload(
                agentId,
                idempotencyKey,
                nonce,
                request
        );

        boolean signatureValid = verifySignature(
                canonicalPayload,
                signatureValue,
                publicKeyPem
        );

        if (!signatureValid) {
            throw new IllegalArgumentException(
                    "Invalid AI agent request signature"
            );
        }

        return sha256(canonicalPayload);
    }

    public String createCanonicalPayload(
            UUID agentId,
            String idempotencyKey,
            String nonce,
            CreatePaymentRequest request
    ) {
        return field("agentId", agentId.toString()) +
                field("idempotencyKey", idempotencyKey) +
                field("nonce", nonce) +
                field("mandateId", request.mandateId().toString()) +
                field("paymentReference", request.paymentReference()) +
                field("amount", request.amount().toPlainString()) +
                field(
                        "currencyCode",
                        request.currencyCode().toUpperCase()
                ) +
                field(
                        "merchantIdentifier",
                        request.merchantIdentifier()
                ) +
                field("merchantName", request.merchantName()) +
                field(
                        "merchantCategoryCode",
                        request.merchantCategoryCode()
                ) +
                field(
                        "merchantCountryCode",
                        request.merchantCountryCode().toUpperCase()
                ) +
                field(
                        "merchantVerified",
                        Boolean.toString(request.merchantVerified())
                ) +
                field(
                        "description",
                        request.description() == null
                                ? ""
                                : request.description()
                ) +
                field(
                        "agentTimestamp",
                        request.agentTimestamp().toString()
                );
    }

    private boolean verifySignature(
            String canonicalPayload,
            String signatureValue,
            String publicKeyPem
    ) {
        try {
            PublicKey publicKey = readPublicKey(publicKeyPem);

            Signature verifier = Signature.getInstance(
                    "SHA256withRSA"
            );

            verifier.initVerify(publicKey);
            verifier.update(
                    canonicalPayload.getBytes(StandardCharsets.UTF_8)
            );

            byte[] signatureBytes = Base64.getDecoder()
                    .decode(signatureValue);

            return verifier.verify(signatureBytes);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to verify AI agent signature",
                    exception
            );
        }
    }

    private PublicKey readPublicKey(String publicKeyPem)
            throws Exception {

        String encodedKey = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

        X509EncodedKeySpec keySpec =
                new X509EncodedKeySpec(keyBytes);

        return KeyFactory.getInstance("RSA")
                .generatePublic(keySpec);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash request payload",
                    exception
            );
        }
    }

    private String field(String name, String value) {
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

        return name + ":" +
                valueBytes.length + ":" +
                value + "\n";
    }
}