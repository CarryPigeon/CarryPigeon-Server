package team.carrypigeon.backend.chat.domain.features.message.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.persistence.DatabaseBackedMentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.support.persistence.DatabaseBackedMessageRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * 消息持久化装配配置。
 * 职责：在 message feature 内装配消息仓储适配器与默认消息实时发布器。
 * 边界：这里只负责 Bean 装配，不承载消息业务规则与 Netty 实现细节。
 */
@Configuration
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessagePersistenceConfiguration {

    /**
     * 创建消息仓储适配器。
     *
     * @param messageDatabaseService 消息数据库服务契约
     * @return 面向领域的消息仓储实现
     */
    @Bean
    public MessageRepository messageRepository(MessageDatabaseService messageDatabaseService) {
        return new DatabaseBackedMessageRepository(messageDatabaseService);
    }

    /**
     * 创建提及仓储适配器。
     *
     * @param mentionDatabaseService 提及数据库服务契约
     * @return 面向领域的提及仓储实现
     */
    @Bean
    public MentionRepository mentionRepository(MentionDatabaseService mentionDatabaseService) {
        return new DatabaseBackedMentionRepository(mentionDatabaseService);
    }

    /**
     * 创建默认空实现实时发布器。
     *
     * @return 未启用 realtime 时的空发布器
     */
    @Bean
    @ConditionalOnMissingBean(MessageRealtimePublisher.class)
    public MessageRealtimePublisher noopMessageRealtimePublisher() {
        return (message, senderSnapshot, recipientAccountIds) -> {
        };
    }

}
