package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.UpdateChannelNotificationPreferenceRequest;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.NotificationPreferenceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationChannelPreferenceCommand;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;

/**
 * 频道级通知偏好 HTTP 入口。
 * 职责：保持频道资源路径，同时把通知偏好协议编排归属 server feature。
 * 边界：只传递账号、频道标识和偏好值，不读取 channel 内部类型。
 */
@RestController
@RequestMapping("/api/channels")
public class ChannelNotificationPreferenceController {

    private final NotificationPreferenceApi notificationPreferenceApi;
    private final RequestAuthenticationContext authRequestContext;

    public ChannelNotificationPreferenceController(
            NotificationPreferenceApi notificationPreferenceApi,
            RequestAuthenticationContext authRequestContext
    ) {
        this.notificationPreferenceApi = notificationPreferenceApi;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 更新当前账号在指定频道中的通知偏好。
     * 副作用：持久化频道级通知偏好。
     */
    @PutMapping("/{channelId}/notification_preference")
    @Operation(summary = "更新频道通知偏好", description = "更新当前账户在指定频道中的通知偏好。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "频道通知偏好更新成功")})
    public ResponseEntity<Void> updateChannelNotificationPreference(
            @PathVariable @Positive(message = "channelId must be greater than 0") long channelId,
            @Valid @RequestBody UpdateChannelNotificationPreferenceRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        notificationPreferenceApi.updateChannelPreference(new UpdateNotificationChannelPreferenceCommand(
                principal.accountId(), channelId, body.mode(), body.mutedUntil()
        ));
        return ResponseEntity.noContent().build();
    }
}
