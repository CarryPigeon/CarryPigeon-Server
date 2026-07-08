package team.carrypigeon.backend.chat.domain.features.user.controller.http;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.GetUserProfileByAccountIdCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.command.UpdateCurrentUserProfileCommand;
import team.carrypigeon.backend.chat.domain.features.user.domain.projection.UserProfileResult;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.PatchCurrentUserProfileRequest;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UpdateCurrentUserEmailRequest;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserBackgroundUploadResponse;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserMeResponse;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserPublicProfileListResponse;
import team.carrypigeon.backend.chat.domain.features.user.controller.dto.UserPublicProfileResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 用户资料 HTTP 入口。
 * 职责：承接当前登录用户资料查询与更新协议请求并返回统一响应模型。
 * 边界：当前阶段不暴露资料创建协议。
 */
@Validated
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户资料", description = "当前登录用户资料读取与更新能力。")
public class UserProfileController {

    private final UserProfileApi userProfileDomainApi;
    private final EmailVerificationCodeService emailVerificationCodeService;
    private final RequestAuthenticationContext authRequestContext;
    private final FileTransferApi fileTransferDomainApi;

    /**
     * 创建用户资料 HTTP 入口。
     *
     * @param userProfileDomainApi 用户资料领域 API
     * @param emailVerificationCodeService 邮箱验证码能力端口
     * @param authRequestContext 请求认证上下文
     * @param fileTransferDomainApi 文件传输领域 API
     */
    public UserProfileController(
            UserProfileApi userProfileDomainApi,
            EmailVerificationCodeService emailVerificationCodeService,
            RequestAuthenticationContext authRequestContext,
            FileTransferApi fileTransferDomainApi
    ) {
        this.userProfileDomainApi = userProfileDomainApi;
        this.emailVerificationCodeService = emailVerificationCodeService;
        this.authRequestContext = authRequestContext;
        this.fileTransferDomainApi = fileTransferDomainApi;
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
            @ApiResponse(responseCode = "200", description = "返回当前用户资料"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "资料不存在")
    })
    public UserMeResponse me(HttpServletRequest request) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        UserProfileResult result = userProfileDomainApi.getCurrentUserProfile(
                new GetCurrentUserProfileCommand(principal.accountId())
        );
        return new UserMeResponse(
                Ids.toString(result.accountId()),
                userProfileDomainApi.getCurrentUserEmail(principal.accountId()),
                result.nickname(),
                result.avatarUrl()
        );
    }

    /**
     * 按账户 ID 查询用户资料。
     *
     * @param accountId 账户 ID
     * @param request 当前 HTTP 请求
     * @return 统一响应包装的用户资料
     */
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
                new GetUserProfileByAccountIdCommand(accountId)
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
     * 更新当前登录用户邮箱。
     *
     * @param request 当前 HTTP 请求
     * @param body 邮箱更新请求
     * @return HTTP 204
     */
    @PutMapping("/me/email")
    @Operation(summary = "更新当前用户邮箱", description = "使用验证码更新当前登录账户邮箱。")
    public ResponseEntity<Void> updateCurrentUserEmail(
            HttpServletRequest request,
            @Valid @RequestBody UpdateCurrentUserEmailRequest body
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        emailVerificationCodeService.verifyCode(body.email(), body.code());
        userProfileDomainApi.updateCurrentUserEmail(principal.accountId(), body.email().trim().toLowerCase());
        return ResponseEntity.noContent().build();
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

    /**
     * 上传并更新当前用户背景图。
     * 副作用：将背景图写入文件传输领域维护的对象位置。
     *
     * @param request 当前 HTTP 请求
     * @param background 背景图 multipart 文件
     * @return 背景图下载地址响应
     */
    @PostMapping(path = "/me/background", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "更新当前用户背景图", description = "上传当前用户背景图并返回下载地址。")
    public UserBackgroundUploadResponse uploadCurrentUserBackground(
            HttpServletRequest request,
            @RequestPart("background") MultipartFile background
    ) {
        AuthenticatedAccount principal = authRequestContext.requirePrincipal(request);
        String shareKey = "profile_bg_" + principal.accountId();
        try {
            fileTransferDomainApi.uploadFile(
                    principal.accountId(),
                    shareKey,
                    background.getContentType(),
                    background.getSize(),
                    background.getInputStream()
            );
        } catch (IOException exception) {
            throw ProblemException.fail("background_upload_read_failed", "failed to read background upload content");
        }
        return new UserBackgroundUploadResponse("/api/files/download/" + shareKey);
    }

    private UserPublicProfileResponse toPublicResponse(UserProfileResult result) {
        return new UserPublicProfileResponse(
                Ids.toString(result.accountId()),
                result.nickname(),
                result.avatarUrl()
        );
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            throw ProblemException.validationFailed("ids must not be blank");
        }
        try {
            return Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(Ids::parse)
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw ProblemException.validationFailed("ids must be decimal snowflake strings");
        }
    }
}
