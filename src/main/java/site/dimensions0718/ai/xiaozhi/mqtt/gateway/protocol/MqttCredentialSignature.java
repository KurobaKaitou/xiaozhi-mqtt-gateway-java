package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

public final class MqttCredentialSignature {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private MqttCredentialSignature() {
    }

    public static String generateBase64Signature(String clientId, String usernameBase64, String secretKey) {
        validateInput(clientId, usernameBase64, secretKey);
        String canonical = clientId + "|" + usernameBase64;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("failed to calculate HMAC signature", exception);
        }
    }

    public static boolean verify(String clientId, String usernameBase64, String secretKey, String expectedSignatureBase64) {
        if (expectedSignatureBase64 == null || expectedSignatureBase64.isBlank()) {
            return false;
        }
        String actual = generateBase64Signature(clientId, usernameBase64, secretKey);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expectedSignatureBase64.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void validateInput(String clientId, String usernameBase64, String secretKey) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        if (usernameBase64 == null || usernameBase64.isBlank()) {
            throw new IllegalArgumentException("usernameBase64 must not be blank");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("secretKey must not be blank");
        }
    }
}
