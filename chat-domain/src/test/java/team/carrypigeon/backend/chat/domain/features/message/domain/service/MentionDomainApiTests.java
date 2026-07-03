package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.query.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MentionDomainApi 契约测试。
 */
@Tag("contract")
class MentionDomainApiTests {

    /**
     * 验证 `listMentions` 在 `capsLimitAndRequestsOneExtraItem` 场景下的测试契约。
     */
    @Test
    @DisplayName("list mentions caps limit and requests one extra item")
    void listMentions_capsLimitAndRequestsOneExtraItem() {
        RecordingMentionRepository repository = new RecordingMentionRepository();
        repository.results = List.of(new Mention(11L, 9L, 5001L, 1002L, "user", 1001L, Instant.parse("2026-04-24T12:00:00Z"), false));
        MentionDomainApi service = new MentionDomainApi(repository);

        var results = service.listMentions(new ListMentionsQuery(1001L, 88L, 99, true, 9L));

        assertEquals(1001L, repository.accountId);
        assertEquals(88L, repository.cursorMentionId);
        assertEquals(51, repository.limit);
        assertEquals(true, repository.unreadOnly);
        assertEquals(9L, repository.channelId);
        assertEquals("11", Long.toString(results.getFirst().mentionId()));
    }

    /**
     * 验证 `markMentionRead` 在 `existingMention` 条件下满足 `succeeds` 的测试契约。
     */
    @Test
    @DisplayName("mark mention read existing mention succeeds")
    void markMentionRead_existingMention_succeeds() {
        RecordingMentionRepository repository = new RecordingMentionRepository();
        repository.markAsReadResult = true;
        MentionDomainApi service = new MentionDomainApi(repository);

        service.markMentionRead(1001L, 11L);

        assertEquals(1001L, repository.markAsReadAccountId);
        assertEquals(11L, repository.markAsReadMentionId);
    }

    /**
     * 验证 `markMentionRead` 在 `missingMention` 条件下满足 `throwsNotFound` 的测试契约。
     */
    @Test
    @DisplayName("mark mention read missing mention throws not found")
    void markMentionRead_missingMention_throwsNotFound() {
        RecordingMentionRepository repository = new RecordingMentionRepository();
        MentionDomainApi service = new MentionDomainApi(repository);

        ProblemException exception = assertThrows(ProblemException.class, () -> service.markMentionRead(1001L, 11L));

        assertEquals("not_found", exception.reason());
    }

    /**
     * 验证 `markMentionsRead` 在 `forwardsFilters` 场景下的测试契约。
     */
    @Test
    @DisplayName("mark mentions read forwards filters")
    void markMentionsRead_forwardsFilters() {
        RecordingMentionRepository repository = new RecordingMentionRepository();
        MentionDomainApi service = new MentionDomainApi(repository);

        service.markMentionsRead(1001L, 88L, 9L);

        assertEquals(1001L, repository.markAllAccountId);
        assertEquals(88L, repository.markAllBeforeMentionId);
        assertEquals(9L, repository.markAllChannelId);
    }

    /**
     * `RecordingMentionRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class RecordingMentionRepository implements MentionRepository {
        private long accountId;
        private Long cursorMentionId;
        private int limit;
        private boolean unreadOnly;
        private Long channelId;
        private List<Mention> results = List.of();
        private boolean markAsReadResult;
        private long markAsReadAccountId;
        private long markAsReadMentionId;
        private long markAllAccountId;
        private Long markAllBeforeMentionId;
        private Long markAllChannelId;

        @Override
        public void save(Mention mention) {
        }

        @Override
        public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
            this.accountId = accountId;
            this.cursorMentionId = cursorMentionId;
            this.limit = limit;
            this.unreadOnly = unreadOnly;
            this.channelId = channelId;
            return results;
        }

        @Override
        public boolean markAsRead(long accountId, long mentionId) {
            this.markAsReadAccountId = accountId;
            this.markAsReadMentionId = mentionId;
            return markAsReadResult;
        }

        @Override
        public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
            this.markAllAccountId = accountId;
            this.markAllBeforeMentionId = beforeMentionId;
            this.markAllChannelId = channelId;
            return 0;
        }
    }
}
