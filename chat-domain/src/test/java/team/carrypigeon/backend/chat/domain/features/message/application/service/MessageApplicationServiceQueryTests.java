package team.carrypigeon.backend.chat.domain.features.message.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MessageApplicationService 查询契约测试。
 * 职责：验证消息历史与搜索查询的应用层编排契约。
 * 边界：不验证 HTTP、Netty 和真实数据库访问，只使用内存替身验证查询语义。
 */
@Tag("contract")
class MessageApplicationServiceQueryTests {

    /**
     * 验证按频道搜索消息时会返回匹配关键字的消息。
     */
    @Test
    @DisplayName("search channel messages valid query returns matching messages")
    void searchChannelMessages_validQuery_returnsMatchingMessages() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.searchResults.add(new ChannelMessage(
                5003L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text",
                "hello body", "[文本消息] hello body", "hello body", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(2)
        ));

        ChannelMessageSearchResult result = fixture.service.searchChannelMessages(
                new SearchChannelMessagesQuery(1001L, 1L, "hello", null, null, null, null, null, 20)
        );

        assertEquals(1, result.messages().size());
        assertEquals("[文本消息] hello body", result.messages().getFirst().previewText());
    }

    /**
     * 验证按频道查询历史消息时会返回倒序消息和下一页游标。
     */
    @Test
    @DisplayName("get channel message history member returns messages and next cursor")
    void getChannelMessageHistory_member_returnsMessagesAndNextCursor() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.history.add(new ChannelMessage(
                5002L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "second", "[文本消息] second", "second", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1)
        ));
        fixture.messageRepository.history.add(new ChannelMessage(
                5001L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1001L, "text", "first", "[文本消息] first", "first", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, null, null, null, 20)
        );

        assertEquals(2, result.messages().size());
        assertEquals(5002L, result.messages().getFirst().messageId());
        assertEquals(null, result.nextCursor());
    }

    /**
     * 验证撤回后的消息仍保留在历史记录中，但输出为已撤回占位文本。
     */
    @Test
    @DisplayName("get channel message history recalled message keeps stable identity with redacted content")
    void getChannelMessageHistory_recalledMessage_keepsStableIdentityWithRedactedContent() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.history.add(new ChannelMessage(
                5008L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "[消息已撤回]", "[消息已撤回]", null, null, null, "recalled",
                MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1)
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, null, null, null, 20)
        );

        assertEquals(1, result.messages().size());
        assertEquals(5008L, result.messages().getFirst().messageId());
        assertEquals("recalled", result.messages().getFirst().status());
        assertEquals("[消息已撤回]", result.messages().getFirst().previewText());
        assertEquals(null, result.messages().getFirst().payload());
    }

    /**
     * 验证扩展消息在历史查询中保持结构化 payload 与 metadata。
     */
    @Test
    @DisplayName("get channel message history extension message keeps payload and metadata")
    void getChannelMessageHistory_extensionMessage_keepsPayloadAndMetadata() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.history.add(new ChannelMessage(
                5009L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "test-extension", "mc bridge", "[插件消息] mc bridge", "mc bridge test-extension",
                "{\"plugin_key\":\"test-extension\",\"message_type\":\"test-extension\",\"payload\":{\"event\":\"player_join\"}}", "{\"trace\":true}", "sent",
                MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(2)
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, null, null, null, 20)
        );

        assertEquals("test-extension", result.messages().getFirst().messageType());
        assertEquals("{\"plugin_key\":\"test-extension\",\"message_type\":\"test-extension\",\"payload\":{\"event\":\"player_join\"}}", result.messages().getFirst().payload());
        assertEquals("{\"trace\":true}", result.messages().getFirst().metadata());
    }

    /**
     * 验证 custom 消息在搜索结果中保持自定义预览文本。
     */
    @Test
    @DisplayName("search channel messages custom message keeps preview text")
    void searchChannelMessages_customMessage_keepsPreviewText() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.searchResults.add(new ChannelMessage(
                5010L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "custom",
                "status card", "[自定义消息] status card", "status card",
                "{\"card\":\"server-status\"}", null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(3)
        ));

        ChannelMessageSearchResult result = fixture.service.searchChannelMessages(
                new SearchChannelMessagesQuery(1001L, 1L, "status", null, null, null, null, null, 20)
        );

        assertEquals(1, result.messages().size());
        assertEquals("custom", result.messages().getFirst().messageType());
        assertEquals("[自定义消息] status card", result.messages().getFirst().previewText());
    }

    /**
     * 验证 system 消息在历史查询中保持稳定预览文本。
     */
    @Test
    @DisplayName("get channel message history system message keeps preview text")
    void getChannelMessageHistory_systemMessage_keepsPreviewText() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channels.put(2L, new team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel(
                2L,
                2L,
                "system",
                "",
                "",
                "",
                "system",
                false,
                MessageApplicationServiceTestSupport.BASE_TIME,
                MessageApplicationServiceTestSupport.BASE_TIME
        ));
        fixture.channelMemberRepository.memberships.put(2L, List.of(
                new team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember(
                        2L,
                        1001L,
                        team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole.MEMBER,
                        MessageApplicationServiceTestSupport.BASE_TIME,
                        null
                )
        ));
        fixture.messageRepository.history.add(new ChannelMessage(
                5011L, MessageApplicationServiceTestSupport.SERVER_ID, 2L, 2L, 1001L, "system",
                "maintenance notice", "[系统消息] maintenance notice", "maintenance notice",
                "{\"severity\":\"info\"}", null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(4)
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 2L, null, null, null, null, 20)
        );

        assertEquals("system", result.messages().getFirst().messageType());
        assertEquals("[系统消息] maintenance notice", result.messages().getFirst().previewText());
    }

    /**
     * 验证搜索结果不会再命中撤回前的内容。
     */
    @Test
    @DisplayName("search channel messages recalled content no longer matches")
    void searchChannelMessages_recalledContent_noLongerMatches() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);

        ChannelMessageSearchResult result = fixture.service.searchChannelMessages(
                new SearchChannelMessagesQuery(1001L, 1L, "hello", null, null, null, null, null, 20)
        );

        assertEquals(0, result.messages().size());
    }

    /**
     * 验证频道不存在时历史查询会返回不存在问题语义。
     */
    @Test
    @DisplayName("get channel message history missing channel throws not found problem")
    void getChannelMessageHistory_missingChannel_throwsNotFoundProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channels.clear();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.getChannelMessageHistory(new GetChannelMessageHistoryQuery(1001L, 9L, null, null, null, null, 20))
        );

        assertEquals("channel does not exist", exception.getMessage());
    }

    @Test
    @DisplayName("get channel message history around mid returns contextual messages")
    void getChannelMessageHistory_aroundMid_returnsContextualMessages() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.history.add(new ChannelMessage(
                5003L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "third", "[文本消息] third", "third", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(2)
        ));
        fixture.messageRepository.history.add(new ChannelMessage(
                5002L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "second", "[文本消息] second", "second", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1)
        ));
        fixture.messageRepository.history.add(new ChannelMessage(
                5001L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1001L, "text", "first", "[文本消息] first", "first", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME
        ));
        fixture.messageRepository.messagesById.put(5002L, fixture.messageRepository.history.get(1));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, 5002L, 1, 1, 20)
        );

        assertEquals(3, result.messages().size());
        assertEquals(5003L, result.messages().get(0).messageId());
        assertEquals(5002L, result.messages().get(1).messageId());
        assertEquals(5001L, result.messages().get(2).messageId());
        assertEquals(null, result.nextCursor());
    }

    @Test
    @DisplayName("search channel messages advanced filters narrow results")
    void searchChannelMessages_advancedFilters_narrowResults() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.messageRepository.searchResults.add(new ChannelMessage(
                5004L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1002L, "text", "hello alpha", "[文本消息] hello alpha", "hello alpha", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(3)
        ));
        fixture.messageRepository.searchResults.add(new ChannelMessage(
                5003L, MessageApplicationServiceTestSupport.SERVER_ID, 1L, 1L, 1003L, "text", "hello beta", "[文本消息] hello beta", "hello beta", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(2)
        ));

        ChannelMessageSearchResult result = fixture.service.searchChannelMessages(
                new SearchChannelMessagesQuery(1001L, 1L, "hello", 5005L, 1002L, "Core:Text", 5005L, 5001L, 20)
        );

        assertEquals(1, result.messages().size());
        assertEquals(5004L, result.messages().getFirst().messageId());
        assertEquals(1002L, fixture.messageRepository.lastSearchSenderAccountId);
        assertEquals("text", fixture.messageRepository.lastSearchDomain);
    }
}
