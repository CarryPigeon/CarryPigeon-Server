package team.carrypigeon.backend.chat.domain.features.user.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.CurrentUserResponse;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetCurrentUserProfileQuery;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.query.GetUserProfileByAccountIdQuery;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.PatchCurrentUserProfileRequest;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserPublicProfileListResponse;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserPublicProfileResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

/**
 * 用户资料 HTTP 入口。
 * 职责：承接当前登录用户资料查询与更新协议请求并返回统一响应模型。
 * 边界：当前阶段不暴露资料创建协议。
 */
@Validated
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
@Tag(name = "用户资料", description = "当前登录用户资料读取与更新能力。")
public class UserProfileController {

    private final UserProfileApi userProfileDomainApi;
    private final RequestAuthenticationContext authRequestContext;
    private final AuthAccountApi authAccountApi;
    /**
     * 创建用户资料 HTTP 入口。
     *
     * @param userProfileDomainApi 用户资料领域 API
     * @param authRequestContext 请求认证上下文
     */
    public UserProfileController(
        UserProfileApi userProfileDomainApi,
        RequestAuthenticationContext authRequestContext, AuthAccountApi authAccountApi
    ) {
        this.userProfileDomainApi = userProfileDomainApi;
        this.authRequestContext = authRequestContext;
        this.authAccountApi = authAccountApi;
    }

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 账户 ID
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的用户资料
     */
    @PreAuthorize("isAuthenticated() and principal.accountId == #accountId")
    @GetMapping("/{accountId}")
    @Operation(summary = "按账户 ID 读取资料", description = "按账户 ID 读取用户公开资料。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回用户公开资料"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "资料不存在")
    })
    public UserPublicProfileResponse getByAccountId(
            @Parameter(description = "目标账户 ID", example = "1001")
            @PathVariable @Positive(message = "accountId must be greater than 0") long accountId,
            HttpServletRequest request
    ) {
        authRequestContext.requirePrincipal(request);
        UserProfileResult result = userProfileDomainApi.getUserProfileByAccountId(
                new GetUserProfileByAccountIdQuery(accountId)
        );
        return toPublicResponse(result);
    }

    /**
     * 按 ID 批量查询用户公开资料。
     *
     * @param request 当前 HTTP 请求
     * @return 公开资料列表外壳
     */
    @GetMapping
    @Operation(summary = "批量读取公开资料", description = "按 `ids` 批量读取用户公开资料，避免客户端 N+1 请求。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回公开资料 items 列表"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "422", description = "ids 缺失或格式非法")
    })
    public UserPublicProfileListResponse list(
            @RequestParam(required = false) String ids,
            HttpServletRequest request
    ) {
        authRequestContext.requirePrincipal(request);
        List<Long> accountIds = parseIds(ids);
        List<UserPublicProfileResponse> result = userProfileDomainApi.getPublicUserProfiles(accountIds).stream()
                .map(this::toPublicResponse)
                .toList();
        return new UserPublicProfileListResponse(result);
    }

    /**
     * 按 v1 协议更新当前登录用户公开资料。
     *
     * @param request 当前 HTTP 请求
     * @param body 用户资料更新请求
     * @return HTTP 204
     */
    @PatchMapping("/me")
    @Operation(summary = "更新当前用户资料", description = "按 v1 字段语义更新当前登录账户资料。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "资料不存在"),
            @ApiResponse(responseCode = "422", description = "请求体字段非法")
    })
    public ResponseEntity<Void> patchCurrentUserProfile(
            HttpServletRequest request,
            @Valid @RequestBody PatchCurrentUserProfileRequest body
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        userProfileDomainApi.updateCurrentUserProfile(
                new UpdateCurrentUserProfileCommand(
                        principal.accountId(),
                        body.username(),
                        body.avatar(),
                        body.brief(),
                        body.sex() == null ? 0L : body.sex(),
                        body.birthday() == null ? 0L : body.birthday()
                )
        );
        return ResponseEntity.noContent().build();
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
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        UserProfileResult profile = userProfileDomainApi.getCurrentUserProfile(
            new GetCurrentUserProfileQuery(principal.accountId())
        );
        return new CurrentUserResponse(
            IdUtil.toString(profile.accountId()),
            authAccountApi.getAccountEmail(principal.accountId()),
            profile.nickname(),
            profile.avatarUrl()
        );
    }

    private UserPublicProfileResponse toPublicResponse(UserProfileResult result) {
        return new UserPublicProfileResponse(
                IdUtil.toString(result.accountId()),
                result.nickname(),
                result.avatarUrl()
        );
    }

    /**
     * 解析用户资料批量查询的账号 ID 列表。
     * 失败语义：缺失或任一 ID 不是十进制雪花 ID 时返回统一校验问题。
     *
     * @param ids 逗号分隔的账号 ID 字符串
     * @return 账号 ID 列表
     */
    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            throw ProblemException.validationFailed("ids must not be blank");
        }
        try {
            return Arrays.stream(ids.split(",", -1))
                    .map(String::trim)
                    .map(value -> {
                        if (value.isBlank()) {
                            throw new IllegalArgumentException("blank id segment");
                        }
                        return IdUtil.parse(value);
                    })
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw ProblemException.validationFailed("ids must be decimal snowflake strings");
        }
    }
}
