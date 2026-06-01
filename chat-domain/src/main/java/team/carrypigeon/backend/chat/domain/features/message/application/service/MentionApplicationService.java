package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MentionResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.ListMentionsQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 提及应用服务。
 * 职责：承接提及列表查询与已读状态更新用例。
 * 边界：这里只编排提及读取和已读变更，不负责提及生成。
 */
@Service
public class MentionApplicationService {

    private final MentionRepository mentionRepository;

    public MentionApplicationService(MentionRepository mentionRepository) {
        this.mentionRepository = mentionRepository;
    }

    /**
     * 查询账户的提及列表。
     * 输入：账户、游标、未读过滤和可选频道范围。
     * 输出：面向控制器的提及结果集合。
     */
    public List<MentionResult> listMentions(ListMentionsQuery query) {
        int normalizedLimit = Math.max(1, Math.min(query.limit(), 50));
        return mentionRepository.listByAccountId(query.accountId(), query.cursorMentionId(), normalizedLimit + 1, query.unreadOnly(), query.channelId()).stream()
                .map(mention -> new MentionResult(mention.mentionId(), mention.channelId(), mention.messageId(), mention.fromAccountId(), mention.targetType(), mention.targetAccountId(), mention.createdAt(), mention.read()))
                .toList();
    }

    /**
     * 把单条提及标记为已读。
     * 失败：当目标提及不存在时返回 not-found。
     */
    public void markMentionRead(long accountId, long mentionId) {
        if (!mentionRepository.markAsRead(accountId, mentionId)) {
            throw ProblemException.notFound("mention not found");
        }
    }

    /**
     * 批量标记提及为已读。
     * 输入：账户、可选上界游标和可选频道过滤范围。
     */
    public void markMentionsRead(long accountId, Long beforeMentionId, Long channelId) {
        mentionRepository.markAllAsRead(accountId, beforeMentionId, channelId);
    }
}
