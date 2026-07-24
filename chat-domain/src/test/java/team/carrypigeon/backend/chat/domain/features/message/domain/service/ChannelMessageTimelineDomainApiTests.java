package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.SearchChannelMessagesQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 消息时间线契约测试。
 * 职责：验证历史与搜索无损返回 canonical envelope，并直接使用完整 domain 过滤。
 */
@Tag("contract")
class ChannelMessageTimelineDomainApiTests {

    /**
     * 验证历史查询不重建或拆分 canonical data。
     */
    @Test
    @DisplayName("history returns canonical message unchanged")
    void getChannelMessageHistory_existingMessage_returnsCanonicalMessage() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.history.add(message(5001L, "Core:ReplyText", Map.of(
                "content", Map.of("text", "reply"), "reply_to_mid", "4999"
        )));

        var result = fixture.timelineApi.getChannelMessageHistory(
                new GetChannelMessageHistoryQuery(1001L, 1L, null, null, null, null, 20)
        );

        assertEquals("Core:ReplyText", result.messages().getFirst().domain());
        assertEquals("4999", result.messages().getFirst().data().get("reply_to_mid"));
        assertNull(result.nextCursor());
    }

    /**
     * 验证搜索过滤直接向仓储传递完整 domain。
     */
    @Test
    @DisplayName("search passes canonical domain filter")
    void searchChannelMessages_domainFilter_passesCanonicalDomain() {
        MessageDomainApiTestSupport.Fixture fixture = new MessageDomainApiTestSupport.Fixture(null);
        fixture.messageRepository.searchResults.add(message(5002L, "Core:Forward", Map.of(
                "forwarded_from", Map.of("mid", "5001")
        )));

        var result = fixture.timelineApi.searchChannelMessages(new SearchChannelMessagesQuery(
                1001L, 1L, "forward", null, null, "Core:Forward", null, null, 20
        ));

        assertEquals("Core:Forward", fixture.messageRepository.lastSearchDomain);
        assertEquals("Core:Forward", result.messages().getFirst().domain());
    }

    private ChannelMessage message(long messageId, String domain, Map<String, Object> data) {
        return new ChannelMessage(
                messageId, 1001L, 1L, domain, "1.0.0", data,
                MessageDomainApiTestSupport.BASE_TIME, List.of(), "forward", MessageStatus.SENT
        );
    }
}
