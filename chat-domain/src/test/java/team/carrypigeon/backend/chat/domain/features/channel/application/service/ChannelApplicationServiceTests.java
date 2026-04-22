package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.application.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ChannelApplicationService 契约测试。
 * 职责：验证默认频道查询用例的应用层编排契约。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
class ChannelApplicationServiceTests {

    /**
     * 验证默认频道存在时会返回稳定频道结果。
     */
    @Test
    @DisplayName("get default channel existing channel returns result")
    void getDefaultChannel_existingChannel_returnsResult() {
        ChannelApplicationService service = new ChannelApplicationService(new FixedChannelRepository(channel()));

        ChannelResult result = service.getDefaultChannel(new GetDefaultChannelCommand(1001L));

        assertEquals(1L, result.channelId());
        assertEquals(1L, result.conversationId());
        assertEquals("public", result.name());
        assertEquals("public", result.type());
    }

    /**
     * 验证默认频道缺失时会返回不存在问题语义。
     */
    @Test
    @DisplayName("get default channel missing channel throws not found problem")
    void getDefaultChannel_missingChannel_throwsNotFoundProblem() {
        ChannelApplicationService service = new ChannelApplicationService(new FixedChannelRepository(null));

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.getDefaultChannel(new GetDefaultChannelCommand(1001L))
        );

        assertEquals("default channel does not exist", exception.getMessage());
    }

    private static Channel channel() {
        Instant now = Instant.parse("2026-04-22T00:00:00Z");
        return new Channel(1L, 1L, "public", "public", true, now, now);
    }

    private static class FixedChannelRepository implements ChannelRepository {

        private final Channel channel;

        private FixedChannelRepository(Channel channel) {
            this.channel = channel;
        }

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channel);
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channel);
        }
    }
}
