package team.carrypigeon.backend.chat.domain.service.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.service.preview.ApiMessagePreviewService;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiWsPayloadMapperTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void channelsChangedPayload_expectedRefreshHint() {
        ApiWsPayloadMapper mapper = newMapper(mock(UserDao.class), mock(FileInfoDao.class), mock(ApiMessagePreviewService.class));

        JsonNode payload = mapper.channelsChangedPayload();

        assertEquals("refresh", payload.path("hint").asText());
        assertEquals(1, payload.size());
    }

    @Test
    void channelChangedPayload_expectedCidScopeHint() {
        ApiWsPayloadMapper mapper = newMapper(mock(UserDao.class), mock(FileInfoDao.class), mock(ApiMessagePreviewService.class));

        JsonNode payload = mapper.channelChangedPayload(123L, "members");

        assertEquals("123", payload.path("cid").asText());
        assertEquals("members", payload.path("scope").asText());
        assertEquals("refresh", payload.path("hint").asText());
    }

    @Test
    void readStateUpdatedPayload_expectedSnowflakeStrings() {
        ApiWsPayloadMapper mapper = newMapper(mock(UserDao.class), mock(FileInfoDao.class), mock(ApiMessagePreviewService.class));

        CPChannelReadState state = new CPChannelReadState()
                .setCid(123L)
                .setUid(456L)
                .setLastReadMid(789L)
                .setLastReadTime(1700000000000L);

        JsonNode payload = mapper.readStateUpdatedPayload(state);

        assertEquals("123", payload.path("cid").asText());
        assertEquals("456", payload.path("uid").asText());
        assertEquals("789", payload.path("last_read_mid").asText());
        assertEquals(1700000000000L, payload.path("last_read_time").asLong());
    }

    @Test
    void messageCreatedPayload_expectedStructuredMessageItem() {
        UserDao userDao = mock(UserDao.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);
        ApiMessagePreviewService previewService = mock(ApiMessagePreviewService.class);
        ApiWsPayloadMapper mapper = newMapper(userDao, fileInfoDao, previewService);

        CPUser user = new CPUser();
        user.setUsername("Alice");
        user.setAvatar(9L);
        when(userDao.getById(2L)).thenReturn(user);

        CPFileInfo avatar = new CPFileInfo();
        avatar.setShareKey("shr_9");
        when(fileInfoDao.getById(9L)).thenReturn(avatar);

        JsonNode data = objectMapper.createObjectNode().put("text", "hello");
        when(previewService.preview("Core:Text", data)).thenReturn("hello");

        LocalDateTime sendTime = LocalDateTime.of(2026, 2, 8, 16, 0, 0);
        CPMessage message = new CPMessage()
                .setId(11L)
                .setUid(2L)
                .setCid(3L)
                .setDomain("Core:Text")
                .setDomainVersion(null)
                .setReplyToMid(10L)
                .setData(data)
                .setSendTime(sendTime);

        JsonNode payload = mapper.messageCreatedPayload(message);

        assertEquals("3", payload.path("cid").asText());
        JsonNode msg = payload.path("message");
        assertEquals("11", msg.path("mid").asText());
        assertEquals("3", msg.path("cid").asText());
        assertEquals("2", msg.path("uid").asText());
        assertEquals("Core:Text", msg.path("domain").asText());
        assertEquals("1.0.0", msg.path("domain_version").asText());
        assertEquals("10", msg.path("reply_to_mid").asText());
        assertEquals(TimeUtil.localDateTimeToMillis(sendTime), msg.path("send_time").asLong());
        assertEquals("hello", msg.path("preview").asText());
        assertEquals("hello", msg.path("data").path("text").asText());

        JsonNode sender = msg.path("sender");
        assertEquals("2", sender.path("uid").asText());
        assertEquals("Alice", sender.path("nickname").asText());
        assertEquals("api/files/download/shr_9", sender.path("avatar").asText());
    }

    @Test
    void messageCreatedPayload_missingUserAndReply_expectedFallbackFields() {
        UserDao userDao = mock(UserDao.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);
        ApiMessagePreviewService previewService = mock(ApiMessagePreviewService.class);
        ApiWsPayloadMapper mapper = newMapper(userDao, fileInfoDao, previewService);

        when(userDao.getById(2L)).thenReturn(null);
        when(previewService.preview("Math:Formula", null)).thenReturn("[Math:Formula]");

        LocalDateTime sendTime = LocalDateTime.of(2026, 2, 8, 16, 0, 0);
        CPMessage message = new CPMessage()
                .setId(11L)
                .setUid(2L)
                .setCid(3L)
                .setDomain("Math:Formula")
                .setDomainVersion("2.0.0")
                .setReplyToMid(0L)
                .setData(null)
                .setSendTime(sendTime);

        JsonNode payload = mapper.messageCreatedPayload(message);
        JsonNode msg = payload.path("message");

        assertEquals("2.0.0", msg.path("domain_version").asText());
        assertEquals("[Math:Formula]", msg.path("preview").asText());
        assertTrue(msg.path("data").isObject());
        assertFalse(msg.has("reply_to_mid"));

        JsonNode sender = msg.path("sender");
        assertEquals("", sender.path("nickname").asText());
        assertEquals("", sender.path("avatar").asText());
    }

    /**
     * 测试辅助方法。
     *
     * @param userDao 测试输入参数
     * @param fileInfoDao 测试输入参数
     * @param previewService 测试输入参数
     * @return 测试辅助方法返回结果
     */
    private ApiWsPayloadMapper newMapper(UserDao userDao, FileInfoDao fileInfoDao, ApiMessagePreviewService previewService) {
        return new ApiWsPayloadMapper(objectMapper, userDao, fileInfoDao, previewService);
    }
}
