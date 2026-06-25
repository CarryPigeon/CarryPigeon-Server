package team.carrypigeon.backend.chat.domain.features.channel.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.application.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.AuditLogResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListAuditLogsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelQueryApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.AuditLogItemResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.CursorPageResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.OpaqueCursorCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 审计日志 HTTP 入口。
 */
@RestController
@RequestMapping("/api/audit_logs")
public class AuditLogController {

    private static final String AUDIT_CURSOR_SCOPE = "audit_logs";

    private final ChannelQueryApplicationService channelQueryApplicationService;
    private final RequestAuthenticationContext authRequestContext;

    public AuditLogController(
            ChannelQueryApplicationService channelQueryApplicationService,
            RequestAuthenticationContext authRequestContext
    ) {
        this.channelQueryApplicationService = channelQueryApplicationService;
        this.authRequestContext = authRequestContext;
    }

    @GetMapping
    public CursorPageResponse<AuditLogItemResponse> listAuditLogs(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(name = "cid", required = false) String channelId,
            @RequestParam(name = "actor_uid", required = false) String actorUid,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "from_time", required = false) Long fromTime,
            @RequestParam(name = "to_time", required = false) Long toTime,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        List<AuditLogResult> items = channelQueryApplicationService.listAuditLogs(new ListAuditLogsQuery(
                principal.accountId(),
                OpaqueCursorCodec.decode(AUDIT_CURSOR_SCOPE, cursor),
                limit,
                parseOptionalSnowflake(channelId, false),
                parseOptionalSnowflake(actorUid, false),
                action,
                fromTime,
                toTime
        ));
        boolean hasMore = items.size() > limit;
        List<AuditLogResult> pageItems = hasMore ? items.subList(0, limit) : items;
        String nextCursor = hasMore ? OpaqueCursorCodec.encode(AUDIT_CURSOR_SCOPE, Long.parseLong(pageItems.get(pageItems.size() - 1).auditId())) : null;
        return CursorPageResponse.of(pageItems.stream().map(item -> new AuditLogItemResponse(
                item.auditId(),
                item.cid(),
                item.actorUid(),
                item.action(),
                item.details(),
                item.createdAt()
        )).toList(), nextCursor, hasMore);
    }

    private Long parseOptionalSnowflake(String rawValue, boolean cursorField) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawValue.trim());
        } catch (RuntimeException exception) {
            if (cursorField) {
                throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
            }
            throw ProblemException.validationFailed("snowflake value is invalid");
        }
    }
}
