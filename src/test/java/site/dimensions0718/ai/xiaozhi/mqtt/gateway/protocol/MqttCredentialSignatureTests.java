package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttCredentialSignatureTests {

    @Test
    void shouldMatchPhaseZeroVector() {
        String clientId = "lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932";
        String usernameBase64 = "eyJpcCI6IjE5Mi4xNjguMS43NyJ9";
        String secret = "AaBbCc123!";

        String actual = MqttCredentialSignature.generateBase64Signature(clientId, usernameBase64, secret);
        assertEquals("Ur2KvFER5uJK3BF4XzgEi75ckLE9liLRN6eGEJK1g7U=", actual);
        assertTrue(MqttCredentialSignature.verify(clientId, usernameBase64, secret, actual));
        assertFalse(MqttCredentialSignature.verify(clientId, usernameBase64, secret,
                "Ur2KvFER5uJK3BF4XzgEi75ckLE9liLRN6eGEJK1g7V="));
    }
}
