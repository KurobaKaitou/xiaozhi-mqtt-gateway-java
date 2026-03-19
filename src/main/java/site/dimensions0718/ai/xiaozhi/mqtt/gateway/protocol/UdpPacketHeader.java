package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public record UdpPacketHeader(
        int type,
        int flags,
        int payloadLength,
        long connectionId,
        long timestamp,
        long sequence
) {

    public static final int HEADER_LENGTH = 16;
    public static final int TYPE_AUDIO = 0x01;

    public UdpPacketHeader {
        if (type < 0 || type > 0xFF) {
            throw new IllegalArgumentException("type must be uint8");
        }
        if (flags < 0 || flags > 0xFF) {
            throw new IllegalArgumentException("flags must be uint8");
        }
        if (payloadLength < 0 || payloadLength > 0xFFFF) {
            throw new IllegalArgumentException("payloadLength must be uint16");
        }
        validateUnsignedInt(connectionId, "connectionId");
        validateUnsignedInt(timestamp, "timestamp");
        validateUnsignedInt(sequence, "sequence");
    }

    public static UdpPacketHeader decode(byte[] packet) {
        if (packet == null || packet.length < HEADER_LENGTH) {
            throw new IllegalArgumentException("packet must contain at least 16 bytes header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet, 0, HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
        int type = Byte.toUnsignedInt(buffer.get());
        int flags = Byte.toUnsignedInt(buffer.get());
        int payloadLength = Short.toUnsignedInt(buffer.getShort());
        long connectionId = Integer.toUnsignedLong(buffer.getInt());
        long timestamp = Integer.toUnsignedLong(buffer.getInt());
        long sequence = Integer.toUnsignedLong(buffer.getInt());
        return new UdpPacketHeader(type, flags, payloadLength, connectionId, timestamp, sequence);
    }

    public static boolean isCompletePacket(byte[] packet) {
        if (packet == null || packet.length < HEADER_LENGTH) {
            return false;
        }
        UdpPacketHeader header = decode(Arrays.copyOf(packet, HEADER_LENGTH));
        return packet.length >= HEADER_LENGTH + header.payloadLength();
    }

    public static byte[] buildNonceHeader(long connectionId) {
        return new UdpPacketHeader(TYPE_AUDIO, 0, 0, connectionId, 0, 0).encode();
    }

    private static void validateUnsignedInt(long value, String field) {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(field + " must be uint32");
        }
    }

    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) type);
        buffer.put((byte) flags);
        buffer.putShort((short) payloadLength);
        buffer.putInt((int) connectionId);
        buffer.putInt((int) timestamp);
        buffer.putInt((int) sequence);
        return buffer.array();
    }
}
