package team.carrypigeon.backend.chat.domain.features.auth.support.security;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * HS256 JWT 鉴权令牌服务。
 * 职责：使用 JDK HMAC 与项目统一 JSON 能力签发和解析最小 JWT。
 * 边界：只支持当前 auth MVP 所需的 access/refresh token，不实现 OAuth/OIDC 能力。
 */
@Component
public class HmacJwtAuthTokenService implements AuthTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuthJwtProperties properties;
    private final JsonProvider jsonProvider;

    public HmacJwtAuthTokenService(AuthJwtProperties properties, JsonProvider jsonProvider) {
        this.properties = properties;
        this.jsonProvider = jsonProvider;
    }

    @Override
    public String issueAccessToken(AuthAccount account, Instant expiresAt) {
        return issueToken(account, 0L, ACCESS_TOKEN_TYPE, expiresAt);
    }

    @Override
    public String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt) {
        return issueToken(account, refreshSessionId, REFRESH_TOKEN_TYPE, expiresAt);
    }

    @Override
    public AuthTokenClaims parseAccessToken(String accessToken) {
        AuthTokenClaims claims = parseToken(accessToken);
        if (!ACCESS_TOKEN_TYPE.equals(claims.tokenType())) {
            throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
        }
        return claims;
    }

    @Override
    public AuthTokenClaims parseRefreshToken(String refreshToken) {
        AuthTokenClaims claims = parseToken(refreshToken);
        if (!REFRESH_TOKEN_TYPE.equals(claims.tokenType())) {
            throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
        }
        return claims;
    }

    private String issueToken(AuthAccount account, long refreshSessionId, String tokenType, Instant expiresAt) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.issuer());
        payload.put("sub", String.valueOf(account.id()));
        payload.put("username", account.username());
        payload.put("typ", tokenType);
        payload.put("sid", refreshSessionId);
        payload.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = base64Url(jsonProvider.toJson(header).getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64Url(jsonProvider.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + base64Url(sign(signingInput));
    }

    private AuthTokenClaims parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw ProblemException.forbidden("invalid_token", "token is invalid");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = base64Url(sign(signingInput));
        if (!MessageDigestSupport.constantTimeEquals(expectedSignature, parts[2])) {
            throw ProblemException.forbidden("invalid_token", "token is invalid");
        }

        Map<String, Object> payload = jsonProvider.fromJson(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
                MAP_TYPE
        );
        if (!properties.issuer().equals(payload.get("iss"))) {
            throw ProblemException.forbidden("invalid_token", "token is invalid");
        }

        Instant expiresAt = Instant.ofEpochSecond(((Number) payload.get("exp")).longValue());
        if (!expiresAt.isAfter(Instant.now())) {
            throw ProblemException.forbidden("token_expired", "token is expired");
        }

        return new AuthTokenClaims(
                String.valueOf(payload.get("sub")),
                String.valueOf(payload.get("username")),
                String.valueOf(payload.get("typ")),
                ((Number) payload.get("sid")).longValue(),
                expiresAt
        );
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw ProblemException.fail("token_sign_failed", "failed to sign token");
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class MessageDigestSupport {

        private MessageDigestSupport() {
        }

        static boolean constantTimeEquals(String left, String right) {
            byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
            byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
            if (leftBytes.length != rightBytes.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < leftBytes.length; i++) {
                result |= leftBytes[i] ^ rightBytes[i];
            }
            return result == 0;
        }
    }
}
