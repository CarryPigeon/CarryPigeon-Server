package team.carrypigeon.backend.chat.domain.features.verification.controller.http;

import java.util.Locale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import team.carrypigeon.backend.chat.domain.http.ClientAddressResolver;
import team.carrypigeon.backend.chat.domain.http.Result;
import team.carrypigeon.backend.chat.domain.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.features.verification.controller.http.dto.SendEmailCodeRequest;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.limit.RateLimitApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 邮箱验证 HTTP 入口。
 * 职责：承接邮箱验证码签发协议并调用 verification 领域 API。
 * 边界：保持既有 `/api/auth/email_codes` 路由，不承载 auth 会话业务。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证与会话", description = "邮箱验证码、会话令牌签发、刷新与撤销。")
public class EmailVerificationController {

    private final EmailVerificationApi emailVerificationApi;
    private final RateLimitApi rateLimitApi;
    private final ClientAddressResolver clientAddressResolver;

    public EmailVerificationController(EmailVerificationApi emailVerificationApi, RateLimitApi rateLimitApi,
            ClientAddressResolver clientAddressResolver) {
        this.emailVerificationApi = emailVerificationApi;
        this.rateLimitApi = rateLimitApi;
        this.clientAddressResolver = clientAddressResolver;
    }

    /**
     * 发送邮箱验证码。
     *
     * <p>请求 {@code POST /api/auth/email_codes}。JSON 请求体示例 {@code {"email":"alice@example.com","purpose":"LOGIN"}}。
     * <p>成功响应 HTTP 200，code 为 0，data 为 {@code null}。
     * <p>失败示例：请求受限 12004，修改邮箱时缺少认证 11001。
     *
     * @param requestBody 验证码请求
     */
    @PermitAll
    @PostMapping(value = "/email_codes", produces = "application/json", consumes = {"application/json", "application/*+json"})
    @Operation(summary = "发送邮箱验证码", description = "按 REGISTER、LOGIN 或 CHANGE_EMAIL 用途签发验证码并受理异步邮件任务；成功不保证收件箱收到，最终投递失败会作废对应批次。CHANGE_EMAIL 必须登录并绑定当前账号。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "成功时 code 为 0 且 data 为 null", useReturnTypeSchema = true))
    public Result<Void> sendEmailCode(
        @Valid
        @RequestBody
        SendEmailCodeRequest requestBody,
        HttpServletRequest request,
        @AuthenticationPrincipal
        CpPrincipal principal
    ) {
        // 在占用发送配额前检查认证，账号绑定仅来自可信认证上下文。
        Long accountId = null;
        if (requestBody.purpose() == EmailCodePurpose.CHANGE_EMAIL) {
            if (principal == null || principal.accountId() == null) {
                throw ProblemException.of(VerificationProblem.AUTHENTICATION_REQUIRED);
            }
            accountId = principal.accountId();
        }
        // 邮件发送频率限制
        String email = requestBody.email().trim().toLowerCase(Locale.ROOT);
        if (!rateLimitApi.tryAcquire("email:code:" + email, 60)) {
            throw ProblemException.of(VerificationProblem.TOO_MANY_REQUESTS);
        }
        // 只按可信代理边界解析出的地址限流，不能让直连客户端自行指定 IP。
        if (!rateLimitApi.tryAcquire("email:code:ip:" + clientAddressResolver.resolve(request), 60)) {
            throw ProblemException.of(VerificationProblem.TOO_MANY_REQUESTS);
        }
        emailVerificationApi.issueCode(email, requestBody.purpose(), accountId);
        return Result.success();
    }
}
