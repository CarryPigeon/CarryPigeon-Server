package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AppendMessageRecallAuditCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * `ChannelMessageAuditDomainApi` 契约测试。
 * 职责：验证消息撤回事实被精确映射为 channel 拥有的审计记录。
 * 边界：不执行消息读取、权限判断或事务提交。
 */
@Tag("contract")
class ChannelMessageAuditDomainApiTests {

    /**
     * 验证撤回审计命令的参与者、动作和结构化元数据完整写入仓储。
     */
    @Test
    @DisplayName("append message recall audit valid command maps record")
    void appendMessageRecallAudit_validCommand_mapsRecord() {
        Instant occurredAt = Instant.parse("2026-07-17T12:00:00Z");
        ChannelAuditLogRepository repository = mock(ChannelAuditLogRepository.class);
        ChannelMessageAuditDomainApi api = new ChannelMessageAuditDomainApi(repository);

        api.appendMessageRecallAudit(new AppendMessageRecallAuditCommand(
                9001L, 1L, 1001L, 5001L, 1002L, occurredAt
        ));

        ArgumentCaptor<ChannelAuditLog> captor = ArgumentCaptor.forClass(ChannelAuditLog.class);
        verify(repository).append(captor.capture());
        assertEquals(new ChannelAuditLog(
                9001L,
                1L,
                1001L,
                "MESSAGE_RECALLED",
                1002L,
                "{\"messageId\":5001,\"senderAccountId\":1002}",
                occurredAt
        ), captor.getValue());
    }
}
