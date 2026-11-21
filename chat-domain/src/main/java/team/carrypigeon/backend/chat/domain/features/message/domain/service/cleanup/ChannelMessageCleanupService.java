package team.carrypigeon.backend.chat.domain.features.message.domain.service.cleanup;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.cleanup.ChannelMessageCleanupApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;

/** 消息侧频道清理实现；清理与成员及频道生命周期关联的数据，权限和事务由调用方负责。 */
@Service
public class ChannelMessageCleanupService implements ChannelMessageCleanupApi {
    private final MessageRepository messageRepository;
    private final MentionRepository mentionRepository;
    private final ChannelReadStateRepository readStateRepository;

    public ChannelMessageCleanupService(MessageRepository messageRepository, MentionRepository mentionRepository,
            ChannelReadStateRepository readStateRepository) {
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.readStateRepository = readStateRepository;
    }

    @Override
    public void deleteChannel(long channelId) {
        readStateRepository.deleteByChannelId(channelId);
        messageRepository.deleteByChannelId(channelId);
    }

    @Override
    public void deleteMemberState(long channelId, long accountId) {
        mentionRepository.deleteByChannelAndRecipient(channelId, accountId);
        readStateRepository.deleteByMember(channelId, accountId);
    }

    @Override
    public void deleteChannelMentions(long channelId) {
        mentionRepository.deleteByChannelId(channelId);
    }
}
