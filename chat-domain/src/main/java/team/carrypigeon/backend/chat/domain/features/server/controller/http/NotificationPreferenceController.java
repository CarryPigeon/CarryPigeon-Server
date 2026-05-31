package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.server.application.command.UpdateNotificationServerPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.NotificationPreferencesResult;
import team.carrypigeon.backend.chat.domain.features.server.application.service.NotificationPreferenceApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.NotificationPreferencesResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.UpdateNotificationPreferenceRequest;

/**
 * 通知偏好 HTTP 入口。
 */
@RestController
@RequestMapping("/api/notification_preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceApplicationService notificationPreferenceApplicationService;
    private final AuthRequestContext authRequestContext;

    public NotificationPreferenceController(
            NotificationPreferenceApplicationService notificationPreferenceApplicationService,
            AuthRequestContext authRequestContext
    ) {
        this.notificationPreferenceApplicationService = notificationPreferenceApplicationService;
        this.authRequestContext = authRequestContext;
    }

    @GetMapping
    public NotificationPreferencesResponse getNotificationPreferences(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        NotificationPreferencesResult result = notificationPreferenceApplicationService.getNotificationPreferences(principal.accountId());
        return new NotificationPreferencesResponse(
                new NotificationPreferencesResponse.ServerNotificationPreferenceResponse(result.server().mode(), result.server().mutedUntil()),
                result.channels().stream()
                        .map(item -> new NotificationPreferencesResponse.ChannelNotificationPreferenceResponse(item.cid(), item.mode(), item.mutedUntil()))
                        .toList()
        );
    }

    @PutMapping("/server")
    public ResponseEntity<Void> updateServerNotificationPreference(
            @RequestBody UpdateNotificationPreferenceRequest body,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        notificationPreferenceApplicationService.updateServerPreference(new UpdateNotificationServerPreferenceCommand(
                principal.accountId(),
                body.mode(),
                body.mutedUntil()
        ));
        return ResponseEntity.noContent().build();
    }
}
