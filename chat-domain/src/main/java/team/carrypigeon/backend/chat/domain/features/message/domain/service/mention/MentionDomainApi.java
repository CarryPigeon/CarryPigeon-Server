package team.carrypigeon.backend.chat.domain.features.message.domain.service.mention;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.MentionApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result.MentionResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 提及领域服务。
 * 职责：承接提及列表查询与已读状态更新用例。
 * 边界：不承载请求字段校验；这里只编排提及读取和已读变更，不负责提及生成。
 */
@Service
public class MentionDomainApi implements MentionApi {

    private final MentionRepository mentionRepository;

    public MentionDomainApi(MentionRepository mentionRepository) {
        this.mentionRepository = mentionRepository;
    }

    @Override
    public List<MentionResult> listMentions(ListMentionsQuery query) {
        return mentionRepository.listByAccountId(query.accountId(), query.cursorMentionId(), query.limit() + 1, query.unreadOnly(), query.channelId()).stream()
                .map(mention -> new MentionResult(mention.mentionId(), mention.channelId(), mention.messageId(), mention.fromAccountId(), mention.targetType(), mention.targetAccountId(), mention.createdAt(), mention.read()))
                .toList();
    }

    @Override
    public void markMentionRead(long accountId, long mentionId) {
        if (!mentionRepository.markAsRead(accountId, mentionId)) {
            throw ProblemException.of(MessageProblem.MENTION_NOT_FOUND);
        }
    }

    @Override
    public void markMentionsRead(long accountId, Long beforeMentionId, Long channelId) {
        mentionRepository.markAllAsRead(accountId, beforeMentionId, channelId);
    }
}
