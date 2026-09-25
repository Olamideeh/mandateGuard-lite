package com.example.mandateguard.security;

import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AgentKeyService {

    public ValidatedPublicKey validateAndFingerprint(
            String publicKeyPem
    ) {
        if (publicKeyPem == null
                || publicKeyPem.isBlank()) {
            throw new IllegalArgumentException(
                    "Public key cannot be empty"
            );
        }

        try {
            String normalizedPem =
                    normalizePem(publicKeyPem);

            byte[] keyBytes = extractKeyBytes(
                    normalizedPem
            );

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            RSAPublicKey publicKey =
                    (RSAPublicKey) keyFactory.generatePublic(
                            new X509EncodedKeySpec(keyBytes)
                    );

            if (publicKey.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException(
                        "RSA public key must contain at least 2048 bits"
                );
            }

            String fingerprint =
                    createFingerprint(keyBytes);

            return new ValidatedPublicKey(
                    normalizedPem,
                    fingerprint
            );

        } catch (IllegalArgumentException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Public key must be a valid RSA public key in PEM format"
            );
        }
    }

    private String normalizePem(String publicKeyPem) {
        String content = publicKeyPem
                .replace("\r", "")
                .trim();

        if (!content.startsWith(
                "-----BEGIN PUBLIC KEY-----"
        ) || !content.endsWith(
                "-----END PUBLIC KEY-----"
        )) {
            throw new IllegalArgumentException(
                    "Public key must use X.509 PEM format"
            );
        }

        return content;
    }

    private byte[] extractKeyBytes(
            String normalizedPem
    ) {
        String encodedKey = normalizedPem
                .replace(
                        "-----BEGIN PUBLIC KEY-----",
                        ""
                )
                .replace(
                        "-----END PUBLIC KEY-----",
                        ""
                )
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(encodedKey);
    }

    private String createFingerprint(
            byte[] keyBytes
    ) throws Exception {
        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        return HexFormat.of().formatHex(
                digest.digest(keyBytes)
        );
    }

    public record ValidatedPublicKey(
            String normalizedPem,
            String fingerprint
    ) {
    }
}