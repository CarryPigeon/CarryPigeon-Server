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
 * Authentication endpoints under {@code /api/auth}.
 * <p>
 * All endpoints in this controller are public (no {@code Authorization} header required) because they are used for
 * onboarding/login flows. The implementation is delegated to LiteFlow chains (see
 * {@code application-starter/src/main/resources/config/api.xml}).
 * <p>
 * Required gate behavior:
 * {@code POST /api/auth/tokens} must perform required gate checks (P0) and reject login if required plugins are missing.
 */
@RestController
public class ApiAuthController {

    private static final String CHAIN_REQUIRED_GATE_CHECK = "api_required_gate_check";
    private static final String CHAIN_AUTH_EMAIL_CODES = "api_auth_email_codes";
    private static final String CHAIN_AUTH_TOKENS = "api_auth_tokens";
    private static final String CHAIN_AUTH_REFRESH = "api_auth_refresh";
    private static final String CHAIN_AUTH_REVOKE = "api_auth_revoke";

    private final ApiFlowRunner flowRunner;

    public ApiAuthController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * Required gate check (public).
     * <p>
     * Route: {@code POST /api/gates/required/check}
     * <p>
     * Chain: {@code api_required_gate_check}
     */
    @PostMapping("/api/gates/required/check")
    public Object requiredGateCheck(@Valid @RequestBody RequiredGateCheckRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_REQUIRED_GATE_CHECK, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Send email login code (public).
     * <p>
     * Route: {@code POST /api/auth/email_codes}
     * <p>
     * Chain: {@code api_auth_email_codes}
     */
    @PostMapping("/api/auth/email_codes")
    public ResponseEntity<Void> emailCodes(@Valid @RequestBody SendEmailCodeRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_EMAIL_CODES, ctx);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exchange email+code for tokens (public).
     * <p>
     * Route: {@code POST /api/auth/tokens}
     * <p>
     * Chain: {@code api_auth_tokens}
     */
    @PostMapping("/api/auth/tokens")
    public Object tokens(@Valid @RequestBody TokenRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_TOKENS, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Refresh tokens (public).
     * <p>
     * Route: {@code POST /api/auth/refresh}
     * <p>
     * Chain: {@code api_auth_refresh}
     */
    @PostMapping("/api/auth/refresh")
    public Object refresh(@Valid @RequestBody RefreshRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_REFRESH, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Revoke refresh token (public).
     * <p>
     * Route: {@code POST /api/auth/revoke}
     * <p>
     * Chain: {@code api_auth_revoke}
     */
    @PostMapping("/api/auth/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, request);
        flowRunner.executeOrThrow(CHAIN_AUTH_REVOKE, ctx);
        return ResponseEntity.noContent().build();
    }
}
