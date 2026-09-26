package com.example.mandateguard;

import com.example.mandateguard.dto.CreatePaymentRequest;
import com.example.mandateguard.service.AgentSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentSignatureServiceTest {

    private AgentSignatureService signatureService;
    private KeyPair keyPair;
    private UUID agentId;
    private String idempotencyKey;
    private String nonce;

    @BeforeEach
    void setUp() throws Exception {
        signatureService = new AgentSignatureService();

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        agentId = UUID.randomUUID();
        idempotencyKey = "idem-" + UUID.randomUUID();
        nonce = UUID.randomUUID().toString();
    }

    @Test
    void shouldAcceptValidAgentSignature()
            throws Exception {

        CreatePaymentRequest request =
                createRequest(new BigDecimal("250.00"));

        String signature = sign(request);

        String payloadHash =
                signatureService.verifyAndHash(
                        agentId,
                        idempotencyKey,
                        nonce,
                        signature,
                        publicKeyPem(),
                        request
                );

        assertThat(payloadHash)
                .matches("[0-9a-f]{64}");
    }

    @Test
    void shouldRejectRequestModifiedAfterSigning()
            throws Exception {

        CreatePaymentRequest originalRequest =
                createRequest(new BigDecimal("250.00"));

        String signature = sign(originalRequest);

        CreatePaymentRequest modifiedRequest =
                createRequest(new BigDecimal("900.00"));

        assertThatThrownBy(() ->
                signatureService.verifyAndHash(
                        agentId,
                        idempotencyKey,
                        nonce,
                        signature,
                        publicKeyPem(),
                        modifiedRequest
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Invalid AI agent request signature"
                );
    }

    @Test
    void shouldRejectChangedNonce()
            throws Exception {

        CreatePaymentRequest request =
                createRequest(new BigDecimal("250.00"));

        String signature = sign(request);

        String differentNonce =
                UUID.randomUUID().toString();

        assertThatThrownBy(() ->
                signatureService.verifyAndHash(
                        agentId,
                        idempotencyKey,
                        differentNonce,
                        signature,
                        publicKeyPem(),
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Invalid AI agent request signature"
                );
    }
    @Test
    void shouldRejectChangedIdempotencyKey()
            throws Exception {

        CreatePaymentRequest request =
                createRequest(new BigDecimal("250.00"));

        String signature = sign(request);

        String differentIdempotencyKey =
                "idem-" + UUID.randomUUID();

        assertThatThrownBy(() ->
                signatureService.verifyAndHash(
                        agentId,
                        differentIdempotencyKey,
                        nonce,
                        signature,
                        publicKeyPem(),
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Invalid AI agent request signature"
                );
    }





    private String sign(CreatePaymentRequest request)
            throws Exception {

        String canonicalPayload =
                signatureService.createCanonicalPayload(
                        agentId,
                        idempotencyKey,
                        nonce,
                        request
                );

        Signature signer =
                Signature.getInstance("SHA256withRSA");

        signer.initSign(keyPair.getPrivate());

        signer.update(
                canonicalPayload.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        return Base64.getEncoder()
                .encodeToString(signer.sign());
    }

    private String publicKeyPem() {
        String encoded = Base64.getMimeEncoder(
                64,
                "\n".getBytes(StandardCharsets.UTF_8)
        ).encodeToString(
                keyPair.getPublic().getEncoded()
        );

        return """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(encoded);
    }

    private CreatePaymentRequest createRequest(
            BigDecimal amount
    ) {
        return new CreatePaymentRequest(
                UUID.randomUUID(),
                "PAY-" + UUID.randomUUID(),
                amount,
                "USD",
                "MERCHANT-001",
                "Test Merchant",
                "3000",
                "US",
                true,
                "Signature test",
                Instant.now()
        );
    }
}