package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UdpPacketHeaderTests {

    private static byte[] hex(String value) {
        int length = value.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return result;
    }

    @Test
    void shouldEncodeAndDecodeHeaderUsingBigEndian() {
        UdpPacketHeader header = new UdpPacketHeader(0x01, 0x00, 16, 0x11223344L, 0x0000003CL, 0x0000000AL);
        byte[] encoded = header.encode();

        assertArrayEquals(hex("01000010112233440000003c0000000a"), encoded);

        UdpPacketHeader decoded = UdpPacketHeader.decode(encoded);
        assertEquals(0x01, decoded.type());
        assertEquals(0x00, decoded.flags());
        assertEquals(16, decoded.payloadLength());
        assertEquals(0x11223344L, decoded.connectionId());
        assertEquals(0x3CL, decoded.timestamp());
        assertEquals(0x0AL, decoded.sequence());
    }

    @Test
    void shouldValidatePacketCompleteness() {
        byte[] header = hex("01000010112233440000003c0000000a");
        byte[] payload = new byte[16];
        byte[] complete = new byte[header.length + payload.length];
        System.arraycopy(header, 0, complete, 0, header.length);
        System.arraycopy(payload, 0, complete, header.length, payload.length);

        assertTrue(UdpPacketHeader.isCompletePacket(complete));
        assertFalse(UdpPacketHeader.isCompletePacket(Arrays.copyOf(complete, complete.length - 1)));
    }

    @Test
    void shouldRejectShortHeader() {
        assertThrows(IllegalArgumentException.class, () -> UdpPacketHeader.decode(new byte[10]));
    }
}
