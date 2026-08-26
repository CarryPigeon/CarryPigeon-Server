package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessageAuditApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AppendMessageRecallAuditCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;

/**
 * 频道消息审计 API 实现。
 * 职责：把消息撤回审计命令转换为 channel 拥有的审计记录。
 */
@Service
public class ChannelMessageAuditDomainApi implements ChannelMessageAuditApi {

    private static final String MESSAGE_RECALLED_AUDIT_ACTION = "MESSAGE_RECALLED";

    private final ChannelAuditLogRepository channelAuditLogRepository;

    public ChannelMessageAuditDomainApi(ChannelAuditLogRepository channelAuditLogRepository) {
        this.channelAuditLogRepository = channelAuditLogRepository;
    }

    @Override
    public void appendMessageRecallAudit(AppendMessageRecallAuditCommand command) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                command.auditLogId(), command.channelId(), command.actorAccountId(),
                MESSAGE_RECALLED_AUDIT_ACTION, command.senderAccountId(),
                "{\"messageId\":" + command.messageId()
                        + ",\"senderAccountId\":" + command.senderAccountId() + "}",
                command.occurredAt()
        ));
    }
}
