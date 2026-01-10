package team.carrypigeon.backend.api.connection.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPPacketTests {

    @Test
    void lombokAccessors_shouldWork() {
        CPPacket packet = new CPPacket()
                .setId(1L)
                .setRoute("/core/test")
                .setData(JsonNodeFactory.instance.objectNode().put("k", "v"));

        assertEquals(1L, packet.getId());
        assertEquals("/core/test", packet.getRoute());
        assertEquals("v", packet.getData().get("k").asText());
        assertTrue(packet.toString().contains("/core/test"));
    }

    @Test
    void jacksonRoundTrip_shouldPreserveFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        CPPacket original = new CPPacket()
                .setId(9L)
                .setRoute("/core/echo")
                .setData(JsonNodeFactory.instance.objectNode().put("x", 1));

        String json = mapper.writeValueAsString(original);
        CPPacket parsed = mapper.readValue(json, CPPacket.class);

        assertEquals(original.getId(), parsed.getId());
        assertEquals(original.getRoute(), parsed.getRoute());
        assertEquals(1, parsed.getData().get("x").asInt());
    }
}

