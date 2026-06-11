package com.questline.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FieldCipherTest {

    private final FieldCipher cipher = new FieldCipher("a-test-crypto-secret-at-least-32-bytes-long");

    @Test
    void roundTripsPlaintext() {
        String secret = "sk-or-v1-abc123-some-openrouter-key";
        assertThat(cipher.decrypt(cipher.encrypt(secret))).isEqualTo(secret);
    }

    @Test
    void producesDifferentCiphertextEachTime() {
        // Random IV per encryption → same plaintext encrypts differently, but both decrypt back.
        String a = cipher.encrypt("same");
        String b = cipher.encrypt("same");
        assertThat(a).isNotEqualTo(b);
        assertThat(cipher.decrypt(a)).isEqualTo("same");
        assertThat(cipher.decrypt(b)).isEqualTo("same");
    }
}
