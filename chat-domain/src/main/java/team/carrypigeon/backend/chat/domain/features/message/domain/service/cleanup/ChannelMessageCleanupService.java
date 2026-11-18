package team.carrypigeon.backend.chat.domain.features.message.domain.service.cleanup;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.cleanup.ChannelMessageCleanupApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;

/** 消息侧频道清理实现；只处理调用方已撤下的频道，不授予或判断频道删除权限。 */
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
        mentionRepository.deleteByChannelId(channelId);
        readStateRepository.deleteByChannelId(channelId);
        messageRepository.deleteByChannelId(channelId);
    }
}
