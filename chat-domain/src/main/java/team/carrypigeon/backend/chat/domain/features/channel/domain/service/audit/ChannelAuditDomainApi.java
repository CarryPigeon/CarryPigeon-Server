package team.carrypigeon.backend.chat.domain.features.channel.domain.service.audit;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit.input.ListAuditLogsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit.result.AuditLogResult;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit.ChannelAuditApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.audit.ChannelAuditActionMapper;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import java.time.Instant;

/**
 * 频道审计查询 API 实现。
 */
@Service
public class ChannelAuditDomainApi implements ChannelAuditApi {

    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelAuditActionMapper channelAuditActionMapper = new ChannelAuditActionMapper();

    public ChannelAuditDomainApi(
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelMemberRepository channelMemberRepository
    ) {
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelMemberRepository = channelMemberRepository;
    }

    @Override
    public List<AuditLogResult> listAuditLogs(ListAuditLogsQuery query) {
        requirePositive(query.accountId(), "accountId");
        if (query.cursorAuditId() != null && query.cursorAuditId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
        if (query.actorAccountId() != null) {
            requirePositive(query.actorAccountId(), "actorAccountId");
        }
        if (query.fromTime() != null && query.fromTime() < 0) {
            throw ProblemException.validationFailed("from_time must be greater than or equal to 0");
        }
        if (query.toTime() != null && query.toTime() < 0) {
            throw ProblemException.validationFailed("to_time must be greater than or equal to 0");
        }
        String actionType = channelAuditActionMapper.normalizeFilterAction(query.action());
        Instant fromTime = query.fromTime() == null ? null : Instant.ofEpochMilli(query.fromTime());
        Instant toTime = query.toTime() == null ? null : Instant.ofEpochMilli(query.toTime());
        List<ChannelAuditLog> logs;
        if (query.channelId() != null) {
            requirePositive(query.channelId(), "channelId");
            requireMember(query.channelId(), query.accountId());
            logs = channelAuditLogRepository.list(
                    query.cursorAuditId(), query.limit() + 1, query.channelId(),
                    query.actorAccountId(), actionType, fromTime, toTime
            );
        } else {
            logs = channelAuditLogRepository.listByChannelIds(
                    query.cursorAuditId(), query.limit() + 1,
                    channelMemberRepository.findChannelIdsByAccountId(query.accountId()),
                    query.actorAccountId(), actionType, fromTime, toTime
            );
        }
        return logs.stream()
                .map(log -> AuditLogResult.from(log, channelAuditActionMapper.toClientAction(log.actionType())))
                .toList();
    }

    private void requireMember(long channelId, long accountId) {
        if (!channelMemberRepository.exists(channelId, accountId)) {
            throw ProblemException.forbidden("not_channel_member", "channel membership is required");
        }
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
