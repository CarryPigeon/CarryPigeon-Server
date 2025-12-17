package team.carrypigeon.backend.dao.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.common.json.JacksonConfiguration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisSerializationTests {

    @Test
    void serializeDeserialize_cpUserTokenWithLocalDateTime_roundTrips() {
        ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();
        RedisSerializer<Object> serializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build();

        CPUserToken token = new CPUserToken(1L, 2L, "token", LocalDateTime.of(2025, 12, 13, 18, 45, 28));
        byte[] bytes = serializer.serialize(token);
        assertNotNull(bytes);

        Object deserialized = serializer.deserialize(bytes);
        CPUserToken result = assertInstanceOf(CPUserToken.class, deserialized);
        assertEquals(token, result);
    }
}

