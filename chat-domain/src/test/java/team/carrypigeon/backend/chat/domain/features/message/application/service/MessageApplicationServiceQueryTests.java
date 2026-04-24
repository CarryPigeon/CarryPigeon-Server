package team.carrypigeon.backend.chat.domain.features.message.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
                5003L, "carrypigeon-local", 1L, 1L, 1002L, "text",
                "hello body", "[文本消息] hello body", "hello body", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(2)
        ));

        ChannelMessageSearchResult result = fixture.service.searchChannelMessages(
                new SearchChannelMessagesQuery(1001L, 1L, "hello", 20)
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
                5002L, "carrypigeon-local", 1L, 1L, 1002L, "text", "second", "[文本消息] second", "second", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME.plusSeconds(1)
        ));
        fixture.messageRepository.history.add(new ChannelMessage(
                5001L, "carrypigeon-local", 1L, 1L, 1001L, "text", "first", "[文本消息] first", "first", null, null, "sent", MessageApplicationServiceTestSupport.BASE_TIME
        ));

        ChannelMessageHistoryResult result = fixture.service.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, 20)
        );

        assertEquals(2, result.messages().size());
        assertEquals(5002L, result.messages().getFirst().messageId());
        assertEquals(5001L, result.nextCursor());
    }

    /**
     * 验证频道不存在时历史查询会返回不存在问题语义。
     */
    @Test
    @DisplayName("get channel message history missing channel throws not found problem")
    void getChannelMessageHistory_missingChannel_throwsNotFoundProblem() {
        MessageApplicationServiceTestSupport.Fixture fixture = new MessageApplicationServiceTestSupport.Fixture(null);
        fixture.channelRepository.channel = null;

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> fixture.service.getChannelMessageHistory(new GetChannelMessageHistoryQuery(1001L, 9L, null, 20))
        );

        assertEquals("channel does not exist", exception.getMessage());
    }
}
