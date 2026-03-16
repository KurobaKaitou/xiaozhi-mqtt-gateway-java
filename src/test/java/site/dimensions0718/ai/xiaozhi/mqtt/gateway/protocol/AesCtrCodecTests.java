package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesCtrCodecTests {

    private static byte[] hex(String value) {
        int length = value.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return result;
    }

    @Test
    void shouldMatchPhaseZeroAesCtrVector() {
        byte[] key = hex("00112233445566778899aabbccddeeff");
        byte[] iv = hex("0102030405060708090a0b0c0d0e0f10");
        byte[] plaintext = hex("48656c6c6f2d7869616f7a68692d6f7075732d6672616d65");
        byte[] expectedCiphertext = hex("f70045d202582bc286db9a20af765e455c07b93f8e05b7a4");

        byte[] encrypted = AesCtrCodec.encrypt(plaintext, key, iv);
        assertArrayEquals(expectedCiphertext, encrypted);

        byte[] decrypted = AesCtrCodec.decrypt(encrypted, key, iv);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void shouldRejectInvalidKeyLength() {
        byte[] shortKey = new byte[8];
        byte[] iv = new byte[16];
        byte[] payload = new byte[]{0x01};

        assertThrows(IllegalArgumentException.class, () -> AesCtrCodec.encrypt(payload, shortKey, iv));
    }
}
