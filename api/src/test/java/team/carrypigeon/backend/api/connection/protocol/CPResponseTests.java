package team.carrypigeon.backend.api.connection.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPResponseTests {

    @Test
    void error_shouldCreateTemplate() {
        CPResponse response = CPResponse.error();
        assertEquals(-1, response.getId());
        assertEquals(100, response.getCode());
        assertNull(response.getData());
    }

    @Test
    void error_withMessage_shouldSetTextData() {
        CPResponse response = CPResponse.error("bad");
        assertEquals(100, response.getCode());
        assertNotNull(response.getData());
        assertEquals("bad", response.getData().get("msg").asText());
    }

    @Test
    void success_shouldCreateTemplate() {
        CPResponse response = CPResponse.success();
        assertEquals(-1, response.getId());
        assertEquals(200, response.getCode());
        assertNull(response.getData());
    }

    @Test
    void authorityError_shouldCreateTemplateAndSupportMessage() {
        CPResponse response = CPResponse.authorityError();
        assertEquals(300, response.getCode());
        assertNull(response.getData());

        CPResponse withMessage = CPResponse.authorityError("nope");
        assertEquals(300, withMessage.getCode());
        assertEquals("nope", withMessage.getData().get("msg").asText());
    }

    @Test
    void pathNotFound_shouldCreateTemplate() {
        CPResponse response = CPResponse.pathNotFound();
        assertEquals(404, response.getCode());
    }

    @Test
    void serverError_shouldCreateTemplateAndSupportMessage() {
        CPResponse response = CPResponse.serverError();
        assertEquals(500, response.getCode());
        assertNull(response.getData());

        CPResponse withMessage = CPResponse.serverError("oops");
        assertEquals(500, withMessage.getCode());
        assertEquals("oops", withMessage.getData().get("msg").asText());
    }

    @Test
    void copy_shouldCloneFields() {
        JsonNode data = CPResponse.success().setTextData("ok").getData();
        CPResponse original = new CPResponse(7L, 200, data);
        CPResponse copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getCode(), copy.getCode());
        assertSame(original.getData(), copy.getData());
    }
}

