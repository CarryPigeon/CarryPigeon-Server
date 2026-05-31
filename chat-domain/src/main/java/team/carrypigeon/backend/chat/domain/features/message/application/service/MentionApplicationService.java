package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MentionResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 提及应用服务。
 */
@Service
public class MentionApplicationService {

    private final MentionRepository mentionRepository;

    public MentionApplicationService(MentionRepository mentionRepository) {
        this.mentionRepository = mentionRepository;
    }

    public List<MentionResult> listMentions(ListMentionsQuery query) {
        int normalizedLimit = Math.max(1, Math.min(query.limit(), 50));
        return mentionRepository.listByAccountId(query.accountId(), query.cursorMentionId(), normalizedLimit + 1, query.unreadOnly(), query.channelId()).stream()
                .map(mention -> new MentionResult(mention.mentionId(), mention.channelId(), mention.messageId(), mention.fromAccountId(), mention.targetType(), mention.targetAccountId(), mention.createdAt(), mention.read()))
                .toList();
    }

    public void markMentionRead(long accountId, long mentionId) {
        if (!mentionRepository.markAsRead(accountId, mentionId)) {
            throw ProblemException.notFound("mention not found");
        }
    }

    public void markMentionsRead(long accountId, Long beforeMentionId, Long channelId) {
        mentionRepository.markAllAsRead(accountId, beforeMentionId, channelId);
    }
}
