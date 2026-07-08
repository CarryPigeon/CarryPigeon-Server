package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
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
@Tag(name = "通知偏好", description = "服务级与频道级通知偏好查询和更新能力。")
public class NotificationPreferenceController {

    private final NotificationPreferenceApi notificationPreferenceDomainApi;
    private final RequestAuthenticationContext authRequestContext;

    /**
     * 创建通知偏好 HTTP 入口。
     *
     * @param notificationPreferenceDomainApi 通知偏好领域 API
     * @param authRequestContext 请求认证上下文
     */
    public NotificationPreferenceController(
            NotificationPreferenceApi notificationPreferenceDomainApi,
            RequestAuthenticationContext authRequestContext
    ) {
        this.notificationPreferenceDomainApi = notificationPreferenceDomainApi;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 查询当前账号通知偏好。
     * 输入：当前 HTTP 请求中的认证主体。
     * 输出：服务级和频道级通知偏好响应。
     *
     * @param request 当前 HTTP 请求
     * @return 当前账号通知偏好响应
     */
    @GetMapping
    public NotificationPreferencesResponse getNotificationPreferences(HttpServletRequest request) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        NotificationPreferencesResult result = notificationPreferenceDomainApi.getNotificationPreferences(principal.accountId());
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
     * @param request 当前 HTTP 请求
     * @return HTTP 204
     */
    @PutMapping("/server")
    public ResponseEntity<Void> updateServerNotificationPreference(
            @Valid @NotNull(message = "request body must not be null") @RequestBody UpdateNotificationPreferenceRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        notificationPreferenceDomainApi.updateServerPreference(new UpdateNotificationServerPreferenceCommand(
                principal.accountId(),
                body.mode(),
                body.mutedUntil()
        ));
        return ResponseEntity.noContent().build();
    }
}
