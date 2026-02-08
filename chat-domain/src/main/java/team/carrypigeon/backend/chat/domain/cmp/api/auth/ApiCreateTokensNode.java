package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.AccessTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.auth.RefreshTokenService;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.TokenRequest;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.common.id.IdUtil;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exchange email+code for access token and refresh token.
 * <p>
 * Route: {@code POST /api/auth/tokens} (public)
 * <p>
 * Input: {@link ApiFlowKeys#REQUEST} = {@link TokenRequest}
 * Output: {@link ApiAuthFlowKeys#TOKEN_RESPONSE} = {@link ApiTokenResponse}
 * <p>
 * P0 responsibilities:
 * <ul>
 *   <li>Enforce required gate: reject login when required plugins are missing.</li>
 *   <li>Validate email code (one-time use) from {@link CPCache}.</li>
 *   <li>Create user on first login.</li>
 *   <li>Issue access token (cache) + refresh token (database).</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("ApiCreateTokens")
@RequiredArgsConstructor
public class ApiCreateTokensNode extends CPNodeComponent {

    private final CpApiProperties properties;
    private final CPCache cache;
    private final UserDao userDao;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof TokenRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        if (!"email_code".equalsIgnoreCase(req.grantType())) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "grant_type", "reason", "invalid", "message", "unsupported grant_type")
                    ))));
        }

        List<String> missing = missingPlugins(req);
        if (!missing.isEmpty()) {
            throw new CPProblemException(CPProblem.of(412, "required_plugin_missing", "required plugins are missing",
                    Map.of("missing_plugins", missing)));
        }

        String serverCode = cache.getAndDelete(emailCodeKey(req.email()));
        if (serverCode == null || !serverCode.equals(req.code())) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", "code", "reason", "invalid", "message", "invalid email code")
                    ))));
        }

        CPUser user = userDao.getByEmail(req.email());
        boolean isNewUser = false;
        if (user == null) {
            isNewUser = true;
            user = new CPUser()
                    .setId(IdUtil.generateId())
                    .setEmail(req.email())
                    .setUsername(defaultUsername(req.email()))
                    .setAvatar(0L)
                    .setSex(CPUserSexEnum.UNKNOWN)
                    .setBrief("")
                    .setBirthday(null)
                    .setRegisterTime(LocalDateTime.now());
            if (!userDao.save(user)) {
                throw new CPProblemException(CPProblem.of(500, "internal_error", "failed to create user"));
            }
        }

        int expiresIn = properties.getAuth().getAccessTokenTtlSeconds();
        String accessToken = accessTokenService.issue(user.getId(), expiresIn);
        CPUserToken refreshToken = refreshTokenService.issue(user.getId(), properties.getAuth().getRefreshTokenTtlDays());

        context.set(ApiAuthFlowKeys.TOKEN_RESPONSE,
                ApiTokenResponse.from(user.getId(), accessToken, expiresIn, refreshToken.getToken(), isNewUser));
        log.debug("ApiCreateTokens success, uid={}, isNewUser={}", user.getId(), isNewUser);
    }

    private List<String> missingPlugins(TokenRequest req) {
        Set<String> installed = new HashSet<>();
        if (req.client() != null && req.client().installedPlugins() != null) {
            req.client().installedPlugins().forEach(p -> installed.add(p.pluginId()));
        }
        return properties.getApi().getRequiredPlugins().stream()
                .filter(p -> !installed.contains(p))
                .toList();
    }

    private String emailCodeKey(String email) {
        return email + ":code";
    }

    private String defaultUsername(String email) {
        int at = email.indexOf('@');
        String prefix = at > 0 ? email.substring(0, at) : email;
        if (prefix.isBlank()) {
            return "user_" + IdUtil.generateId();
        }
        return prefix;
    }

}
