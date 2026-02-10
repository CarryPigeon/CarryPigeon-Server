package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.*;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * 认证相关 API 控制器。
 * <p>
 * 提供邮箱验证码、令牌签发/刷新/吊销、required gate 检查等路由。
 */
@RestController
public class ApiAuthController {

    private static final String CHAIN_REQUIRED_GATE_CHECK = "api_required_gate_check";
    private static final String CHAIN_AUTH_EMAIL_CODES = "api_auth_email_codes";
    private static final String CHAIN_AUTH_TOKENS = "api_auth_tokens";
    private static final String CHAIN_AUTH_REFRESH = "api_auth_refresh";
    private static final String CHAIN_AUTH_REVOKE = "api_auth_revoke";

    private final ApiFlowRunner flowRunner;

    /**
     * 创建认证控制器。
     *
     * @param flowRunner API 链路执行器
     */
    public ApiAuthController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * 执行 required gate 预检查。
     * <p>
     * Route: {@code POST /api/gates/required/check}
     * Chain: {@code api_required_gate_check}
     *
     * @param request required gate 检查请求体
     * @return 标准响应对象（包含 required gate 检查结果）
     */
    @PostMapping("/api/gates/required/check")
    public Object requiredGateCheck(@Valid @RequestBody RequiredGateCheckRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_REQUIRED_GATE_CHECK, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 发送邮箱验证码。
     * <p>
     * Route: {@code POST /api/auth/email_codes}
     * Chain: {@code api_auth_email_codes}
     *
     * @param request 邮箱验证码发送请求体
     * @return 空内容响应（HTTP 204）
     */
    @PostMapping("/api/auth/email_codes")
    public ResponseEntity<Void> emailCodes(@Valid @RequestBody SendEmailCodeRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_EMAIL_CODES, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * 使用邮箱验证码换取访问令牌与刷新令牌。
     * <p>
     * Route: {@code POST /api/auth/tokens}
     * Chain: {@code api_auth_tokens}
     *
     * @param request 令牌签发请求体
     * @return 标准响应对象（包含 access_token 与 refresh_token）
     */
    @PostMapping("/api/auth/tokens")
    public Object tokens(@Valid @RequestBody TokenRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_TOKENS, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 刷新访问令牌与刷新令牌。
     * <p>
     * Route: {@code POST /api/auth/refresh}
     * Chain: {@code api_auth_refresh}
     *
     * @param request 令牌刷新请求体
     * @return 标准响应对象（包含新的 access_token 与 refresh_token）
     */
    @PostMapping("/api/auth/refresh")
    public Object refresh(@Valid @RequestBody RefreshRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_REFRESH, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 吊销刷新令牌。
     * <p>
     * Route: {@code POST /api/auth/revoke}
     * Chain: {@code api_auth_revoke}
     *
     * @param request 令牌吊销请求体
     * @return 空内容响应（HTTP 204）
     */
    @PostMapping("/api/auth/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_REVOKE, ctx);
        return ResponseEntity.noContent().build();
    }
}
