package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTokenServiceTests {

    @Test
    void shouldValidateBearerTokenForCurrentDate() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-16T08:00:00Z"), ZoneId.of("UTC"));
        ApiTokenService service = new ApiTokenService("AaBbCc123!", fixedClock);

        String token = service.generateToken(LocalDate.of(2026, 3, 16));
        assertTrue(service.isValidAuthorizationHeader("Bearer " + token));
        assertFalse(service.isValidAuthorizationHeader("Bearer bad-token"));
    }

    @Test
    void shouldRejectWhenHeaderMissingOrKeyBlank() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-16T08:00:00Z"), ZoneId.of("UTC"));
        ApiTokenService blankKeyService = new ApiTokenService("", fixedClock);
        assertFalse(blankKeyService.isValidAuthorizationHeader("Bearer token"));
        assertFalse(blankKeyService.isValidAuthorizationHeader(null));
    }
}
