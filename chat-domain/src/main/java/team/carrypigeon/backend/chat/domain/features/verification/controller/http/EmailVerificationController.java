package team.carrypigeon.backend.chat.domain.features.verification.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.verification.controller.dto.SendEmailCodeRequest;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.IssueEmailVerificationCodeCommand;

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

    public EmailVerificationController(EmailVerificationApi emailVerificationApi) {
        this.emailVerificationApi = emailVerificationApi;
    }

    /**
     * 发送邮箱验证码。
     *
     * @param request 验证码请求
     * @return HTTP 204
     */
    @PostMapping("/email_codes")
    @Operation(summary = "发送邮箱验证码", description = "为目标邮箱签发一次性验证码。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "验证码请求受理成功")
    })
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        emailVerificationApi.issueCode(new IssueEmailVerificationCodeCommand(request.email()));
        return ResponseEntity.noContent().build();
    }
}
