package team.carrypigeon.backend.chat.domain.features.auth.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.CurrentUserResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.UpdateCurrentAccountEmailRequest;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.UpdateCurrentAccountEmailCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetCurrentUserProfileQuery;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 当前账号 HTTP 入口。
 * 职责：组合认证账号与公开用户资料，并承接邮箱登录标识更新协议。
 * 边界：保持 `/api/users/me` 协议，不把账号语义下放到 user feature。
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户资料", description = "当前登录账号及其公开资料能力。")
public class CurrentUserAccountController {

    private final AuthAccountApi authAccountApi;
    private final UserProfileApi userProfileApi;
    private final RequestAuthenticationContext authenticationContext;

    public CurrentUserAccountController(
            AuthAccountApi authAccountApi,
            UserProfileApi userProfileApi,
            RequestAuthenticationContext authenticationContext
    ) {
        this.authAccountApi = authAccountApi;
        this.userProfileApi = userProfileApi;
        this.authenticationContext = authenticationContext;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    @Operation(summary = "读取当前用户资料", description = "返回当前 access token 对应账户的资料信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回当前用户资料"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "资料不存在")
    })
    public CurrentUserResponse me(HttpServletRequest request) {
        AuthenticatedAccount principal = authenticationContext.requirePrincipal(request);
        UserProfileResult profile = userProfileApi.getCurrentUserProfile(
                new GetCurrentUserProfileQuery(principal.accountId())
        );
        return new CurrentUserResponse(
                IdUtil.toString(profile.accountId()),
                authAccountApi.getAccountEmail(principal.accountId()),
                profile.nickname(),
                profile.avatarUrl()
        );
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/email")
    @Operation(summary = "更新当前用户邮箱", description = "使用验证码更新当前登录账户邮箱。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "邮箱更新成功")})
    public ResponseEntity<Void> updateEmail(
            HttpServletRequest request,
            @Valid @RequestBody UpdateCurrentAccountEmailRequest body
    ) {
        AuthenticatedAccount principal = authenticationContext.requirePrincipal(request);
        authAccountApi.updateCurrentAccountEmail(new UpdateCurrentAccountEmailCommand(
                principal.accountId(),
                body.email(),
                body.code()
        ));
        return ResponseEntity.noContent().build();
    }
}
