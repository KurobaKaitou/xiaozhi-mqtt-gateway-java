package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Service
public class ApiTokenService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final String signatureKey;
    private final Clock clock;

    @Autowired
    public ApiTokenService(@Value("${gateway.security.signature-key:${MQTT_SIGNATURE_KEY:}}") String signatureKey) {
        this(signatureKey, Clock.systemDefaultZone());
    }

    ApiTokenService(String signatureKey, Clock clock) {
        this.signatureKey = signatureKey == null ? "" : signatureKey;
        this.clock = clock;
    }

    public boolean isValidAuthorizationHeader(String authorizationHeader) {
        if (signatureKey.isBlank()) {
            return false;
        }
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            return false;
        }
        String expected = generateToken(LocalDate.now(clock));
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(LocalDate date) {
        if (signatureKey.isBlank()) {
            throw new IllegalStateException("signature key is blank");
        }
        String input = date.format(DATE_FORMATTER) + signatureKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
