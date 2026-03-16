package site.dimensions0718.ai.xiaozhi.mqtt.gateway.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.config.GatewayRuntimeProperties;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol.MqttCredentialSignature;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.InMemoryDeviceSessionStore;
import site.dimensions0718.ai.xiaozhi.mqtt.gateway.session.SessionState;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class MqttControlServiceImplTests {

    @Test
    void shouldHandleHelloAndCreateSession() {
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        GatewayRuntimeProperties properties = new GatewayRuntimeProperties();
        properties.setUdpPublicHost("127.0.0.1");
        properties.setUdpPort(8884);
        String secret = "AaBbCc123!";
        MqttControlServiceImpl service = new MqttControlServiceImpl(store, properties, secret, new SecureRandom());

        String clientId = "lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932";
        String username = "eyJpcCI6IjE5Mi4xNjguMS43NyJ9";
        String password = MqttCredentialSignature.generateBase64Signature(clientId, username, secret);
        String payload = """
                {
                  "type":"hello",
                  "version":3,
                  "transport":"udp",
                  "audio_params":{"format":"opus","sample_rate":16000,"channels":1,"frame_duration":60}
                }
                """;

        String response = service.handlePublish(clientId, username, password, payload);
        JSONObject json = JSON.parseObject(response);

        assertEquals("hello", json.getString("type"));
        assertEquals("udp", json.getString("transport"));
        assertEquals(3, json.getIntValue("version"));
        assertEquals("aes-128-ctr", json.getJSONObject("udp").getString("encryption"));
        assertTrue(store.findByClientId(clientId).isPresent());
        assertEquals(SessionState.UDP_ACTIVE, store.findByClientId(clientId).orElseThrow().state());
    }

    @Test
    void shouldReplaceOldSessionWhenSameClientReconnects() {
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        GatewayRuntimeProperties properties = new GatewayRuntimeProperties();
        String secret = "AaBbCc123!";
        MqttControlServiceImpl service = new MqttControlServiceImpl(store, properties, secret, new SecureRandom());

        String clientId = "lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932";
        String username = "eyJpcCI6IjE5Mi4xNjguMS43NyJ9";
        String password = MqttCredentialSignature.generateBase64Signature(clientId, username, secret);
        String hello = "{" +
                "\"type\":\"hello\"," +
                "\"version\":3," +
                "\"transport\":\"udp\"," +
                "\"audio_params\":{\"format\":\"opus\",\"sample_rate\":16000,\"channels\":1,\"frame_duration\":60}" +
                "}";

        String first = service.handlePublish(clientId, username, password, hello);
        String firstSessionId = JSON.parseObject(first).getString("session_id");

        String second = service.handlePublish(clientId, username, password, hello);
        String secondSessionId = JSON.parseObject(second).getString("session_id");

        assertEquals(1, store.snapshot().size());
        assertNotEquals(firstSessionId, secondSessionId);
    }

    @Test
    void shouldHandleGoodbyeAndRemoveSession() {
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        GatewayRuntimeProperties properties = new GatewayRuntimeProperties();
        String secret = "AaBbCc123!";
        MqttControlServiceImpl service = new MqttControlServiceImpl(store, properties, secret, new SecureRandom());

        String clientId = "lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932";
        String username = "eyJpcCI6IjE5Mi4xNjguMS43NyJ9";
        String password = MqttCredentialSignature.generateBase64Signature(clientId, username, secret);

        service.handlePublish(clientId, username, password,
                "{\"type\":\"hello\",\"version\":3,\"transport\":\"udp\",\"audio_params\":{}}"
        );
        assertTrue(store.findByClientId(clientId).isPresent());

        String response = service.handlePublish(clientId, username, password,
                "{\"type\":\"goodbye\",\"session_id\":\"x\"}"
        );

        assertEquals("goodbye", JSON.parseObject(response).getString("type"));
        assertTrue(store.findByClientId(clientId).isEmpty());
    }

    @Test
    void shouldRejectInvalidSignature() {
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        GatewayRuntimeProperties properties = new GatewayRuntimeProperties();
        MqttControlServiceImpl service = new MqttControlServiceImpl(store, properties, "AaBbCc123!", new SecureRandom());

        assertThrows(IllegalArgumentException.class,
                () -> service.handlePublish(
                        "lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932",
                        "eyJpcCI6IjE5Mi4xNjguMS43NyJ9",
                        "invalid",
                        "{\"type\":\"hello\",\"version\":3,\"transport\":\"udp\",\"audio_params\":{}}"
                )
        );
    }

    @Test
    void shouldHandleBrokerPublishWithoutCredentialSignature() {
        InMemoryDeviceSessionStore store = new InMemoryDeviceSessionStore();
        GatewayRuntimeProperties properties = new GatewayRuntimeProperties();
        properties.setUdpPublicHost("127.0.0.1");
        properties.setUdpPort(8884);
        MqttControlServiceImpl service = new MqttControlServiceImpl(store, properties, "unused", new SecureRandom());

        String clientId = "lichuang-dev@@@a0_85_e3_f4_49_34@@@aeebef32-f0ef-4bce-9d8a-894d91bc6932";
        String response = service.handleBrokerPublish(clientId,
                "{\"type\":\"hello\",\"version\":3,\"transport\":\"udp\",\"audio_params\":{}}"
        );

        assertEquals("hello", JSON.parseObject(response).getString("type"));
        assertTrue(store.findByClientId(clientId).isPresent());
    }
}
