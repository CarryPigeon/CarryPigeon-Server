package team.carrypigeon.backend.chat.domain.features.user.controller.http;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result.PublicAccountResult;
import team.carrypigeon.backend.chat.domain.features.user.controller.http.dto.*;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.input.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.profile.result.UserProfileResult;

/**
 * 用户资料 HTTP 入口。
 * 职责：承接当前登录用户资料查询与更新协议请求并返回统一响应模型。
 * 边界：当前阶段不暴露资料创建协议。
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
@Tag(name = "用户资料", description = "当前登录用户资料读取与更新能力。")
public class UserProfileController {

    private final UserProfileApi userProfileDomainApi;
    private final AuthAccountApi authAccountApi;
    /**
     * 创建用户资料 HTTP 入口。
     *
     * @param userProfileDomainApi 用户资料领域 API
     * @param authAccountApi       认证账户 API
     */
    public UserProfileController(
        UserProfileApi userProfileDomainApi,
        AuthAccountApi authAccountApi
    ) {
        this.userProfileDomainApi = userProfileDomainApi;
        this.authAccountApi = authAccountApi;
    }

    /**
     * 按账户 ID 查询用户详细资料；登录邮箱属于公开资料，任意已认证用户均可读取。
     *
     * <p>请求 {@code GET /api/users/1001/detail}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code UserDetailProfileResponse}。
     * <p>失败示例：资料不存在 50001。
     *
     * @param accountId 账户 ID
     */
    @GetMapping(value = "/{accountId}/detail", produces = "application/json")
    @Operation(summary = "按账户 ID 读取资料", description = "任意已认证用户可读取公开资料，包含目标用户的登录邮箱。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回用户公开资料", useReturnTypeSchema = true))
    public Result getByAccountId(
        @Parameter(description = "目标账户 ID", example = "1001")
        @PathVariable
        @Positive(message = "accountId must be greater than 0")
        long accountId
    ) {
        UserProfileResult result = userProfileDomainApi.findById(accountId);
        PublicAccountResult account = authAccountApi.getAccount(accountId);
        return Result.success(new UserDetailProfileResponse(
                result.accountId() + "",
                result.username(),
                result.avatarUrl(),
                account.email(),
                result.bio(),
                result.sex(),
                result.birthday()
        ));
    }

    /**
     * 按 ID 批量查询用户公开简略资料，主要用于群聊列表展示
     *
     * <p>请求 {@code GET /api/users?ids=1001,1002}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code UserPublicProfileListResponse}。
     * <p>失败示例：请求参数错误 10001。
     *
     * @param ids 要查询的账户 ID 列表
     */
    @GetMapping(produces = "application/json")
    @Operation(summary = "批量读取公开资料", description = "按 `ids` 批量读取用户公开资料，避免客户端 N+1 请求。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回公开资料 items 列表", useReturnTypeSchema = true))
    public Result list(
        @RequestParam
        List<Long> ids
    ) {
        List<UserPublicProfileResponse> result = userProfileDomainApi.getPublicUserProfiles(ids).stream()
                .map(user -> new UserPublicProfileResponse(
                        String.valueOf(user.accountId()),
                        user.username(),
                        user.avatarUrl()
                    )
                )
                .toList();
        return Result.success(new UserPublicProfileListResponse(result));
    }

    /**
     * 更新当前登录用户公开资料。
     *
     * <p>请求 {@code PATCH /api/users/me}。JSON 请求体示例 {@code {"avatar":"avatars/alice.png","brief":"Hello","sex":0,"birthday":0}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：资料不存在 50001。
     *
     * @param body 用户资料更新请求
     */
    @PatchMapping(value = "/me", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "更新当前用户资料", description = "按 v1 字段语义更新当前登录账户资料。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result updateUserProfile(
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal,
        @Valid
        @RequestBody
        PatchCurrentUserProfileRequest body
    ) {
        userProfileDomainApi.updateUserProfile(
                cpPrincipal.accountId(),
                new UpdateCurrentUserProfileCommand(
                    body.avatar(),
                    body.brief(),
                    body.sex() == null ? 0L : body.sex(),
                    body.birthday() == null ? 0L : body.birthday()
                )
        );
        return Result.success();
    }

    /**
     * 读取当前认证账号的资料。
     *
     * <p>请求 {@code GET /api/users/me}。无请求体。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code UserDetailProfileResponse}。
     * <p>失败示例：账号不存在 11009。
     *
     * @param cpPrincipal 当前登录用户
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/me", produces = "application/json")
    @Operation(summary = "读取当前用户资料", description = "返回当前 access token 对应账户的资料信息。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "返回当前用户资料", useReturnTypeSchema = true))
    public Result me(
        @AuthenticationPrincipal
        CpPrincipal cpPrincipal
    ) {
        return this.getByAccountId(cpPrincipal.accountId());
    }
}
