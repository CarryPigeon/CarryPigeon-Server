package team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit.result.AuditLogResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit.input.ListAuditLogsQuery;

/**
 * 频道审计查询领域 API。
 */
public interface ChannelAuditApi {

    List<AuditLogResult> listAuditLogs(ListAuditLogsQuery query);
}
