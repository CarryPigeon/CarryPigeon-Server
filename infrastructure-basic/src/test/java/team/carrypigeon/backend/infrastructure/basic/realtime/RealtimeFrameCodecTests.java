package team.carrypigeon.backend.infrastructure.basic.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;

/**
 * 验证实时通道下行帧的线上格式与上行 envelope 解析。
 * 说明：这里复用生产同款 Jackson 定制器，因此全局命名策略或空值策略变化会直接让本测试失败。
 */
class RealtimeFrameCodecTests {

    private final JsonProviderImpl jsonProvider = productionJsonProvider();
    private final RealtimeFrameCodec frameCodec = new RealtimeFrameCodec(jsonProvider);

    /**
     * 验证事件帧编码为固定四键 envelope，且事件字段为 snake_case。
     */
    @Test
    void encode_eventFrame_producesFourKeyEnvelope() {
        JsonNode frame = jsonProvider.readTree(frameCodec.encode(new RealtimeFrame.Event(
                "9001",
                new RealtimeEventType("message.created"),
                1700000000000L,
                Map.of("cid", "1")
        )));

        assertEquals(RealtimeFrameCodec.EVENT_FRAME_TYPE, frame.get("type").asText());
        assertTrue(frame.has("id"));
        assertTrue(frame.get("id").isNull());
        assertEquals("9001", frame.get("data").get("event_id").asText());
        assertEquals("message.created", frame.get("data").get("event_type").asText());
        assertEquals(1700000000000L, frame.get("data").get("server_time").asLong());
        assertEquals("1", frame.get("data").get("payload").get("cid").asText());
        assertTrue(frame.has("error"));
        assertTrue(frame.get("error").isNull());
    }

    /**
     * 验证命令错误帧的类型带 .err 后缀，错误体只含 reason 与 message。
     */
    @Test
    void encode_commandError_producesTypeErrEnvelope() {
        JsonNode frame = jsonProvider.readTree(frameCodec.encode(new RealtimeFrame.CommandError(
                "1",
                "auth",
                "unauthorized",
                "authentication is required"
        )));

        assertEquals("auth" + RealtimeFrameCodec.ERROR_TYPE_SUFFIX, frame.get("type").asText());
        assertEquals("1", frame.get("id").asText());
        assertTrue(frame.get("data").isNull());
        assertEquals("unauthorized", frame.get("error").get("reason").asText());
        assertEquals("authentication is required", frame.get("error").get("message").asText());
    }

    /**
     * 验证无错误应答帧保留请求 ID 并携带载荷。
     */
    @Test
    void encode_ack_producesAckEnvelope() {
        JsonNode frame = jsonProvider.readTree(frameCodec.encode(new RealtimeFrame.Ack(
                "pong",
                "2",
                Map.of("server_time", 123L)
        )));

        assertEquals("pong", frame.get("type").asText());
        assertEquals("2", frame.get("id").asText());
        assertEquals(123L, frame.get("data").get("server_time").asLong());
        assertTrue(frame.get("error").isNull());
    }

    /**
     * 验证客户端 envelope 能被完整解析。
     */
    @Test
    void decode_clientFrame_readsEnvelopeFields() {
        RealtimeClientMessage message = frameCodec.decode("""
                {"type":"ping","id":"7","ts":1700000000000,"data":{"access_token":"t"}}
                """);

        assertEquals("ping", message.type());
        assertEquals("7", message.id());
        assertEquals(1700000000000L, message.ts());
        assertEquals("t", message.accessToken());
    }

    /**
     * 验证续传锚点从 resume 嵌套对象中读取。
     */
    @Test
    void decode_clientFrame_lastEventId_readsNestedResume() {
        RealtimeClientMessage message = frameCodec.decode("""
                {"type":"auth","id":"1","data":{"resume":{"last_event_id":"9001"}}}
                """);

        assertEquals("9001", message.lastEventId());
    }

    /**
     * 验证 resume 不是对象时按协议校验失败抛出实时协议异常。
     */
    @Test
    void decode_resumeNotObject_throwsProtocolException() {
        RealtimeClientMessage message = frameCodec.decode("""
                {"type":"auth","id":"1","data":{"resume":"oops"}}
                """);

        RealtimeProtocolException exception = assertThrows(RealtimeProtocolException.class, message::lastEventId);
        assertEquals("validation_failed", exception.reason());
    }

    /**
     * 验证缺少 type 的 envelope 被拒绝。
     */
    @Test
    void validate_blankType_throwsProtocolException() {
        RealtimeClientMessage message = frameCodec.decode("""
                {"type":"   ","id":"1","data":{}}
                """);

        RealtimeProtocolException exception = assertThrows(
                RealtimeProtocolException.class,
                () -> frameCodec.validate(message)
        );
        assertEquals("type_blank", exception.reason());
    }

    /**
     * 验证空 envelope 被拒绝而不是抛出空指针异常。
     */
    @Test
    void validate_nullMessage_throwsProtocolException() {
        RealtimeProtocolException exception = assertThrows(
                RealtimeProtocolException.class,
                () -> frameCodec.validate(null)
        );

        assertNotNull(exception);
        assertEquals("frame_invalid", exception.reason());
    }

    private static JsonProviderImpl productionJsonProvider() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonAutoConfiguration().snakeCaseJacksonCustomizer().customize(builder);
        return new JsonProviderImpl(builder.build());
    }
}
