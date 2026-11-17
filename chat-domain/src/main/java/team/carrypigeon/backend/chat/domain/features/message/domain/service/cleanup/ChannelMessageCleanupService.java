package team.carrypigeon.backend.chat.domain.features.message.domain.service.cleanup;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.cleanup.ChannelMessageCleanupApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;

/** 消息侧频道清理实现；只处理调用方已撤下的频道，不授予或判断频道删除权限。 */
@Service
public class ChannelMessageCleanupService implements ChannelMessageCleanupApi {
    private final MessageRepository messageRepository;

    public ChannelMessageCleanupService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public boolean deleteChannelBatch(long channelId, int limit) {
        return messageRepository.deleteChannelBatch(channelId, limit);
    }
}
