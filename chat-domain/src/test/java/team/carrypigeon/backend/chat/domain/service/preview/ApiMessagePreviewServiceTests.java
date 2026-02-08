package team.carrypigeon.backend.chat.domain.service.preview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiMessagePreviewServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preview_coreText_shouldTruncate() throws Exception {
        ApiMessagePreviewService svc = new ApiMessagePreviewService();
        var data = objectMapper.readTree("{\"text\":\"1234567890123456789012345\"}");
        String p = svc.preview("Core:Text", data);
        assertEquals(20, p.length());
        assertTrue(p.startsWith("1234567890"));
    }

    @Test
    void preview_pluginDomain_noPayloadPreview_shouldReturnDomainMarker() throws Exception {
        ApiMessagePreviewService svc = new ApiMessagePreviewService();
        var data = objectMapper.readTree("{\"x\":1}");
        assertEquals("[Math:Formula]", svc.preview("Math:Formula", data));
    }

    @Test
    void preview_pluginDomain_withPayloadPreview_shouldStillReturnDomainMarker() throws Exception {
        ApiMessagePreviewService svc = new ApiMessagePreviewService();
        var data = objectMapper.readTree("{\"preview\":\"hello\\nworld\"}");
        assertEquals("[Math:Formula]", svc.preview("Math:Formula", data));
    }
}
