package team.carrypigeon.backend.common.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JacksonConfigurationTests {

    private record TestRecord(String someValue, LocalDateTime time) {
    }

    @Test
    void objectMapper_shouldApplyExpectedDefaults() throws Exception {
        ObjectMapper mapper = new JacksonConfiguration().objectMapper();

        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        assertEquals(PropertyNamingStrategies.SNAKE_CASE, mapper.getPropertyNamingStrategy());

        String json = mapper.writeValueAsString(new TestRecord("v", LocalDateTime.of(2020, 1, 2, 3, 4, 5)));
        assertTrue(json.contains("some_value"));
        assertTrue(json.contains("time"));
    }
}

