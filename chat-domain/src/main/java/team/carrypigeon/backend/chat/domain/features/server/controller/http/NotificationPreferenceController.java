package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationServerPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.NotificationPreferencesResult;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.NotificationPreferenceApi;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.NotificationPreferencesResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.UpdateNotificationPreferenceRequest;

/**
 * 通知偏好 HTTP 入口。
 */
@RestController
@RequestMapping("/api/notification_preferences")
@PreAuthorize("isAuthenticated()")
@Tag(name = "通知偏好", description = "服务级与频道级通知偏好查询和更新能力。")
public class NotificationPreferenceController {

    private final NotificationPreferenceApi notificationPreferenceDomainApi;

    /**
     * 创建通知偏好 HTTP 入口。
     *
     * @param notificationPreferenceDomainApi 通知偏好领域 API
     */
    public NotificationPreferenceController(
            NotificationPreferenceApi notificationPreferenceDomainApi
    ) {
        this.notificationPreferenceDomainApi = notificationPreferenceDomainApi;
    }

    /**
     * 查询当前账号通知偏好。
     * 输入：当前 HTTP 请求中的认证主体。
     * 输出：服务级和频道级通知偏好响应。
     *
     * @return 当前账号通知偏好响应
     */
    @GetMapping
    public NotificationPreferencesResponse getNotificationPreferences(@AuthenticationPrincipal CpPrincipal cpPrincipal) {
        NotificationPreferencesResult result = notificationPreferenceDomainApi.getNotificationPreferences(cpPrincipal.accountId());
        return new NotificationPreferencesResponse(
                new NotificationPreferencesResponse.ServerNotificationPreferenceResponse(result.server().mode(), result.server().mutedUntil()),
                result.channels().stream()
                        .map(item -> new NotificationPreferencesResponse.ChannelNotificationPreferenceResponse(item.cid(), item.mode(), item.mutedUntil()))
                        .toList()
        );
    }

    /**
     * 更新当前账号服务级通知偏好。
     * 副作用：持久化服务级通知偏好。
     *
     * @param body 通知偏好更新请求
     * @return HTTP 204
     */
    @PutMapping("/server")
    @Operation(summary = "更新服务通知偏好", description = "更新当前账户的服务级通知偏好。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "服务通知偏好更新成功")})
    public ResponseEntity<Void> updateServerNotificationPreference(
            @Valid @NotNull(message = "request body must not be null") @RequestBody UpdateNotificationPreferenceRequest body,
            @AuthenticationPrincipal CpPrincipal cpPrincipal
    ) {
        notificationPreferenceDomainApi.updateServerPreference(new UpdateNotificationServerPreferenceCommand(
                cpPrincipal.accountId(),
                body.mode(),
                body.mutedUntil()
        ));
        return ResponseEntity.noContent().build();
    }
}
