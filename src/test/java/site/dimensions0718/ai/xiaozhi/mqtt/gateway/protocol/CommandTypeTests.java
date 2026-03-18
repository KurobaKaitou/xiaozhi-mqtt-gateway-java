package site.dimensions0718.ai.xiaozhi.mqtt.gateway.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandTypeTests {

    @Test
    void shouldParseMcpTypeCaseInsensitively() {
        assertEquals(CommandType.MCP, CommandType.from("mcp"));
        assertEquals(CommandType.MCP, CommandType.from("MCP"));
    }

    @Test
    void shouldReturnUnknownForInvalidType() {
        assertEquals(CommandType.UNKNOWN, CommandType.from(null));
        assertEquals(CommandType.UNKNOWN, CommandType.from(""));
        assertEquals(CommandType.UNKNOWN, CommandType.from("gps_report"));
    }
}
