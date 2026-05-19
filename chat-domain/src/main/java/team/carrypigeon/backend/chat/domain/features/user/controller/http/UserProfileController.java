package team.carrypigeon.backend.chat.domain.features.user.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.user.application.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfilePageResult;
import team.carrypigeon.backend.chat.domain.features.user.application.dto.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.application.query.GetUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.application.query.SearchUserProfilesQuery;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UpdateCurrentUserProfileRequest;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserProfilePageResponse;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserProfileResponse;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 用户资料 HTTP 入口。
 * 职责：承接当前登录用户资料查询与更新协议请求并返回统一响应模型。
 * 边界：当前阶段不暴露资料创建协议。
 */
@Validated
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户资料", description = "当前登录用户资料读取、分页、搜索与更新能力。")
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
    @Operation(summary = "读取当前用户资料", description = "返回当前 access token 对应账户的资料信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；未认证时通常返回 `CPResponse.code=300`；资料不存在时可能返回 `CPResponse.code=404`")
    })
    public CPResponse<UserProfileResponse> me(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        UserProfileResult result = userProfileApplicationService.getCurrentUserProfile(
                new GetCurrentUserProfileCommand(principal.accountId())
        );
        return CPResponse.success(toResponse(result));
    }

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 账户 ID
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的用户资料
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "按账户 ID 读取资料", description = "按账户 ID 读取资料；当前实现仅允许访问当前登录账户自己的资料。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；访问他人资料时通常返回 `CPResponse.code=300`；资料不存在时可能返回 `CPResponse.code=404`")
    })
    public CPResponse<UserProfileResponse> getByAccountId(
            @Parameter(description = "目标账户 ID；当前实现仅允许传当前登录账户自己的 ID", example = "1001")
            @PathVariable @Positive(message = "accountId must be greater than 0") long accountId,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        requireSameAccount(principal, accountId);
        UserProfileResult result = userProfileApplicationService.getUserProfileByAccountId(
                new GetUserProfileByAccountIdCommand(accountId)
        );
        return CPResponse.success(toResponse(result));
    }

    /**
     * 查询全部用户资料。
     *
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的用户资料列表
     */
    @GetMapping
    @Operation(summary = "读取资料列表", description = "返回当前登录账户当前可见的用户资料列表。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；该接口返回的是资料数组，而不是分页对象")
    })
    public CPResponse<List<UserProfileResponse>> list(HttpServletRequest request) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        List<UserProfileResponse> result = userProfileApplicationService.listUserProfiles(principal.accountId()).stream()
                .map(this::toResponse)
                .toList();
        return CPResponse.success(result);
    }

    /**
     * 查询用户资料分页。
     *
     * @param cursor 游标账户 ID
     * @param limit 查询条数
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的用户资料分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页读取资料", description = "按 accountId 游标分页读取当前登录账户可见的用户资料。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；`data` 为 `users + nextCursor` 分页对象；参数不合法时通常返回 `CPResponse.code=200`")
    })
    public CPResponse<UserProfilePageResponse> page(
            @Parameter(description = "排他游标；为空表示从最新可见资料开始分页", example = "1000")
            @RequestParam(required = false) @Positive(message = "cursor must be greater than 0") Long cursor,
            @Parameter(description = "单页条数，范围 1..100", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be between 1 and 100")
            @Max(value = 100, message = "limit must be between 1 and 100") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        UserProfilePageResult result = userProfileApplicationService.getUserProfiles(
                new GetUserProfilesQuery(principal.accountId(), cursor, limit)
        );
        return CPResponse.success(toPageResponse(result));
    }

    /**
     * 按关键字搜索用户资料。
     *
     * @param keyword 搜索关键字
     * @param cursor 游标账户 ID
     * @param limit 查询条数
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的用户资料分页结果
     */
    @GetMapping("/search")
    @Operation(summary = "搜索资料", description = "按关键字分页搜索当前登录账户可见的用户资料。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；`data` 为搜索结果分页对象；keyword 为空或分页参数非法时通常返回 `CPResponse.code=200`")
    })
    public CPResponse<UserProfilePageResponse> search(
            @Parameter(description = "搜索关键字；用于匹配昵称或简介", example = "pigeon")
            @RequestParam String keyword,
            @Parameter(description = "排他游标；为空表示从最新匹配结果开始", example = "1000")
            @RequestParam(required = false) @Positive(message = "cursor must be greater than 0") Long cursor,
            @Parameter(description = "返回条数，范围 1..100", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be between 1 and 100")
            @Max(value = 100, message = "limit must be between 1 and 100") int limit,
            HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = authRequestContext.requirePrincipal(request);
        UserProfilePageResult result = userProfileApplicationService.searchUserProfiles(
                new SearchUserProfilesQuery(principal.accountId(), keyword, cursor, limit)
        );
        return CPResponse.success(toPageResponse(result));
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param request 当前 HTTP 请求
     * @param body 更新请求
     * @return 统一响应包装的更新后资料
     */
    @PutMapping("/me")
    @Operation(summary = "更新当前用户资料", description = "更新当前登录账户的昵称、头像地址与简介。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "当前用户资料更新请求体。允许更新昵称、头像地址和简介。头像与简介可为空串，但不能为 null。", required = true,
            content = @Content(schema = @Schema(implementation = UpdateCurrentUserProfileRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "业务成功时 `CPResponse.code=100`；请求体字段非法时通常返回 `CPResponse.code=200`；未认证或资料不可访问时通常返回 `CPResponse.code=300/404`")
    })
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

    private UserProfilePageResponse toPageResponse(UserProfilePageResult result) {
        return new UserProfilePageResponse(
                result.users().stream().map(this::toResponse).toList(),
                result.nextCursor()
        );
    }

    private void requireSameAccount(AuthenticatedPrincipal principal, long targetAccountId) {
        if (principal.accountId() != targetAccountId) {
            throw ProblemException.forbidden(
                    "user_profile_access_forbidden",
                    "user profile access is restricted to current account"
            );
        }
    }
}
