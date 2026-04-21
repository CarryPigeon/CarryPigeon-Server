package team.carrypigeon.backend.chat.domain.features.user.controller.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.user.application.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UpdateCurrentUserProfileRequest;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserProfileResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;

/**
 * 用户资料 HTTP 入口。
 * 职责：承接当前登录用户资料查询与更新协议请求并返回统一响应模型。
 * 边界：当前阶段不暴露资料创建、搜索或批量操作协议。
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileApplicationService userProfileApplicationService;
    private final AuthRequestContext authRequestContext;

    public UserProfileController(
            UserProfileApplicationService userProfileApplicationService,
            AuthRequestContext authRequestContext
    ) {
        this.userProfileApplicationService = userProfileApplicationService;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的用户资料
     */
    @GetMapping("/me")
    public CPResponse<UserProfileResponse> me(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        UserProfileResult result = userProfileApplicationService.getCurrentUserProfile(
                new GetCurrentUserProfileCommand(principal.accountId())
        );
        return CPResponse.success(toResponse(result));
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param request 当前 HTTP 请求
     * @param body 更新请求
     * @return 统一响应包装的更新后资料
     */
    @PutMapping("/me")
    public CPResponse<UserProfileResponse> updateMe(
            HttpServletRequest request,
            @Valid @RequestBody UpdateCurrentUserProfileRequest body
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        UserProfileResult result = userProfileApplicationService.updateCurrentUserProfile(
                new UpdateCurrentUserProfileCommand(principal.accountId(), body.nickname(), body.avatarUrl(), body.bio())
        );
        return CPResponse.success(toResponse(result));
    }

    private UserProfileResponse toResponse(UserProfileResult result) {
        return new UserProfileResponse(
                result.accountId(),
                result.nickname(),
                result.avatarUrl(),
                result.bio(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
