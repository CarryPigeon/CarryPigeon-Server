package team.carrypigeon.backend.chat.domain.features.verification.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.carrypigeon.backend.chat.domain.features.verification.controller.http.dto.SendEmailCodeRequest;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.limit.RateLimitApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;

import java.util.Locale;

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

    public EmailVerificationController(EmailVerificationApi emailVerificationApi, RateLimitApi rateLimitApi) {
        this.emailVerificationApi = emailVerificationApi;
        this.rateLimitApi = rateLimitApi;
    }

    /**
     * 发送邮箱验证码。
     *
     * @param requestBody 验证码请求
     * @return HTTP 204
     */
    @PermitAll
    @PostMapping("/email_codes")
    @Operation(summary = "发送邮箱验证码", description = "为目标邮箱签发一次性验证码。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "验证码请求受理成功")
    })
    public ResponseEntity<Void> sendEmailCode(
        @Valid @RequestBody
        SendEmailCodeRequest requestBody,
        HttpServletRequest request
    ) {
        // 邮件发送频率限制
        String email = requestBody.email().trim().toLowerCase(Locale.ROOT);
        if (!rateLimitApi.tryAcquire("email:code:" + email, 60)) {
            return ResponseEntity.status(429).build();
        }
        // ip限制
        if (!rateLimitApi.tryAcquire("email:code:ip:" + getClientIp(request), 60)) {
            return ResponseEntity.status(429).build();
        }
        emailVerificationApi.issueCode(email);
        return ResponseEntity.noContent().build();
    }
    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // X-Forwarded-For 可能包含多个 IP（客户端 + 各级代理），第一个通常是真实客户端
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip;
        }
        // 兜底：返回直连 IP
        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

}
