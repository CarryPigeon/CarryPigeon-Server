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
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * HS256 JWT 鉴权令牌 codec 实现。
 * 职责：使用 JDK HMAC 与项目统一 JSON 能力签发和解析最小 JWT。
 * 边界：只支持当前 auth MVP 所需的 access/refresh token，不实现 OAuth/OIDC 能力。
 */
@Component
public class HmacJwtAuthTokenService implements AuthTokenCodec {

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

    /**
     * 签发 access token。
     * 输入：当前账户和绝对过期时间。
     * 输出：符合当前项目最小 JWT 约束的 access token 字符串。
     *
     * @param account 当前鉴权账号
     * @param expiresAt access token 绝对过期时间
     * @return access token 字符串
     */
    @Override
    public String issueAccessToken(AuthAccount account, Instant expiresAt) {
        return issueToken(account, 0L, ACCESS_TOKEN_TYPE, expiresAt);
    }

    /**
     * 签发 refresh token。
     * 输入：当前账户、refresh session ID 和绝对过期时间。
     * 输出：符合当前项目最小 JWT 约束的 refresh token 字符串。
     *
     * @param account 当前鉴权账号
     * @param refreshSessionId refresh session ID
     * @param expiresAt refresh token 绝对过期时间
     * @return refresh token 字符串
     */
    @Override
    public String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt) {
        return issueToken(account, refreshSessionId, REFRESH_TOKEN_TYPE, expiresAt);
    }

    /**
     * 解析并校验 access token。
     * 失败：类型不匹配、签名错误或过期时抛出统一业务异常。
     *
     * @param accessToken 客户端提交的 access token
     * @return access token 中的账号与过期声明
     */
    @Override
    public AuthTokenClaims parseAccessToken(String accessToken) {
        AuthTokenClaims claims = parseToken(accessToken);
        if (!ACCESS_TOKEN_TYPE.equals(claims.tokenType())) {
            throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
        }
        return claims;
    }

    /**
     * 解析并校验 refresh token。
     * 失败：类型不匹配、签名错误或过期时抛出统一业务异常。
     *
     * @param refreshToken 客户端提交的 refresh token
     * @return refresh token 中的账号、session 与过期声明
     */
    @Override
    public AuthTokenClaims parseRefreshToken(String refreshToken) {
        AuthTokenClaims claims = parseToken(refreshToken);
        if (!REFRESH_TOKEN_TYPE.equals(claims.tokenType())) {
            throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
        }
        return claims;
    }

    /**
     * 组装并签名当前服务端最小 JWT。
     * 输入：账号、token 类型、refresh session ID 和绝对过期时间。
     * 输出：由 header、payload、signature 组成的 HS256 JWT 字符串。
     *
     * @param account token 归属账号
     * @param refreshSessionId refresh token 使用的 session ID；access token 固定为 0
     * @param tokenType token 类型，当前只允许 access 或 refresh
     * @param expiresAt token 绝对过期时间
     * @return 已签名 JWT 字符串
     */
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

    /**
     * 解析并校验 JWT 的通用结构、签名、签发方和过期时间。
     * 失败语义：结构非法、签名不匹配、签发方不匹配或已过期时抛出鉴权问题。
     *
     * @param token 客户端提交的 JWT
     * @return 已校验的 token 声明
     */
    private AuthTokenClaims parseToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw invalidToken();
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw invalidToken();
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = base64Url(sign(signingInput));
            if (!MessageDigestSupport.constantTimeEquals(expectedSignature, parts[2])) {
                throw invalidToken();
            }

            Map<String, Object> payload = jsonProvider.fromJson(
                    new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
                    MAP_TYPE
            );
            if (!properties.issuer().equals(payload.get("iss"))) {
                throw invalidToken();
            }

            Instant expiresAt = Instant.ofEpochSecond(requiredLongClaim(payload, "exp"));
            if (!expiresAt.isAfter(Instant.now())) {
                throw ProblemException.forbidden("token_expired", "token is expired");
            }

            String subject = requiredStringClaim(payload, "sub");
            requirePositiveAccountSubject(subject);
            return new AuthTokenClaims(
                    subject,
                    requiredStringClaim(payload, "username"),
                    requiredStringClaim(payload, "typ"),
                    requiredLongClaim(payload, "sid"),
                    expiresAt
            );
        } catch (ProblemException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
    }

    /**
     * 读取 JWT payload 中必填字符串声明。
     * 失败语义：声明缺失、类型错误或空白时统一按非法 token 处理。
     *
     * @param payload JWT payload
     * @param claimName 声明名
     * @return 非空字符串声明值
     */
    private String requiredStringClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalidToken();
        }
        return text;
    }

    /**
     * 读取 JWT payload 中必填数字声明。
     * 失败语义：声明缺失或类型错误时统一按非法 token 处理。
     *
     * @param payload JWT payload
     * @param claimName 声明名
     * @return long 声明值
     */
    private long requiredLongClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (!(value instanceof Number number)) {
            throw invalidToken();
        }
        return number.longValue();
    }

    /**
     * 校验 token subject 可作为本服务端账号 ID 使用。
     * 失败语义：subject 不是正数账号 ID 时统一按非法 token 处理。
     *
     * @param subject token subject 声明
     */
    private void requirePositiveAccountSubject(String subject) {
        try {
            if (Long.parseLong(subject) <= 0L) {
                throw invalidToken();
            }
        } catch (NumberFormatException exception) {
            throw invalidToken();
        }
    }

    private ProblemException invalidToken() {
        return ProblemException.forbidden("invalid_token", "token is invalid");
    }

    /**
     * 对 JWT signing input 生成 HS256 签名。
     * 失败语义：JDK 加密能力不可用或密钥初始化失败时抛出内部问题。
     *
     * @param signingInput 待签名的 header 与 payload 组合
     * @return 原始 HMAC 签名字节
     */
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

    /**
     * 消息摘要比较支撑。
     * 职责：为 JWT 签名校验提供固定时间字符串比较，避免短路比较暴露签名差异位置。
     */
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
