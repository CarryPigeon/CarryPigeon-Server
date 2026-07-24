package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 消息提醒元数据协作对象。
 * 职责：规范化提醒用户 ID，并从 canonical mentions 生成派生 mention 索引。
 * 边界：不从派生索引反向重建消息 mentions。
 */
class MessageMentionManager {

    private final MentionRepository mentionRepository;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    MessageMentionManager(MentionRepository mentionRepository, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.mentionRepository = mentionRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    List<Long> normalizeMentions(List<Long> mentions) {
        if (mentions == null || mentions.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long mention : mentions) {
            if (mention == null || mention <= 0L) {
                throw ProblemException.validationFailed("mentions must contain positive snowflake ids");
            }
            normalized.add(mention);
        }
        return List.copyOf(normalized);
    }

    List<Mention> persistMentions(ChannelMessage message, List<Long> recipientAccountIds) {
        Set<Long> validRecipients = Set.copyOf(recipientAccountIds);
        java.util.ArrayList<Mention> created = new java.util.ArrayList<>();
        for (Long targetAccountId : message.mentions()) {
            if (targetAccountId == message.senderId() || !validRecipients.contains(targetAccountId)) {
                throw ProblemException.validationFailed("mentions contains user outside message recipients");
            }
            Mention mention = new Mention(
                    idGenerator.nextLongId(),
                    message.channelId(),
                    message.messageId(),
                    message.senderId(),
                    "user",
                    targetAccountId,
                    timeProvider.nowInstant(),
                    false
            );
            mentionRepository.save(mention);
            created.add(mention);
        }
        return List.copyOf(created);
    }

    void deleteByMessageId(long messageId) {
        mentionRepository.deleteByMessageId(messageId);
    }
}
