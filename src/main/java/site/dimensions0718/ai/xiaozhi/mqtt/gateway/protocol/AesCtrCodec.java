package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public final class AesCtrCodec {

    private static final String TRANSFORMATION = "AES/CTR/NoPadding";
    private static final int KEY_BYTES = 16;
    private static final int IV_BYTES = 16;

    private AesCtrCodec() {
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) {
        return process(Cipher.ENCRYPT_MODE, plaintext, key, iv);
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) {
        return process(Cipher.DECRYPT_MODE, ciphertext, key, iv);
    }

    private static byte[] process(int mode, byte[] input, byte[] key, byte[] iv) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (key == null || key.length != KEY_BYTES) {
            throw new IllegalArgumentException("AES-128 key must be 16 bytes");
        }
        if (iv == null || iv.length != IV_BYTES) {
            throw new IllegalArgumentException("CTR IV must be 16 bytes");
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("AES-CTR process failed", exception);
        }
    }
}
