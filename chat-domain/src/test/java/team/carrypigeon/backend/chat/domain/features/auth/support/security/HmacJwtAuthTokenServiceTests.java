package team.carrypigeon.backend.chat.domain.features.auth.support.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HmacJwtAuthTokenService 契约测试。
 * 职责：验证 JWT 解析对非法结构和声明类型返回稳定鉴权问题。
 * 边界：不验证完整加密算法矩阵，只覆盖调用链容易泄漏为 500 的 payload 形态。
 */
@Tag("contract")
class HmacJwtAuthTokenServiceTests {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-09-09T10:00:00Z");

    /**
     * 验证签名正确但缺少 exp 声明的 token 会返回 invalid_token，而不是未分类运行时异常。
     */
    @Test
    @DisplayName("parse access token missing exp throws invalid token problem")
    void parseAccessToken_missingExp_throwsInvalidTokenProblem() {
        HmacJwtAuthTokenService service = service();
        Map<String, Object> payload = validPayload();
        payload.remove("exp");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.parseAccessToken(signedToken(payload))
        );

        assertEquals("invalid_token", exception.reason());
        assertEquals("token is invalid", exception.getMessage());
    }

    /**
     * 验证签名正确但 subject 不是正数账号 ID 的 token 会返回 invalid_token。
     */
    @Test
    @DisplayName("parse access token non numeric subject throws invalid token problem")
    void parseAccessToken_nonNumericSubject_throwsInvalidTokenProblem() {
        HmacJwtAuthTokenService service = service();
        Map<String, Object> payload = validPayload();
        payload.put("sub", "not-a-number");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.parseAccessToken(signedToken(payload))
        );

        assertEquals("invalid_token", exception.reason());
        assertEquals("token is invalid", exception.getMessage());
    }

    /**
     * 验证过期时间等于统一当前时刻时会稳定返回 token_expired。
     */
    @Test
    @DisplayName("parse access token expiry at current time throws token expired problem")
    void parseAccessToken_expiryAtCurrentTime_throwsTokenExpiredProblem() {
        HmacJwtAuthTokenService service = service();
        Map<String, Object> payload = validPayload();
        payload.put("exp", NOW.getEpochSecond());

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.parseAccessToken(signedToken(payload))
        );

        assertEquals("token_expired", exception.reason());
        assertEquals("token is expired", exception.getMessage());
    }

    private HmacJwtAuthTokenService service() {
        return new HmacJwtAuthTokenService(
                new AuthJwtProperties("carrypigeon", SECRET, Duration.ofMinutes(30), Duration.ofDays(14)),
                jsonProvider(),
                new TimeProviderImpl(Clock.fixed(NOW, ZoneOffset.UTC))
        );
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", "carrypigeon");
        payload.put("sub", "1001");
        payload.put("username", "carry-user");
        payload.put("typ", "access");
        payload.put("sid", 0L);
        payload.put("exp", NOW.plusSeconds(3600).getEpochSecond());
        return payload;
    }

    private String signedToken(Map<String, Object> payload) {
        JsonProviderImpl jsonProvider = jsonProvider();
        String header = base64Url(jsonProvider.toJson(Map.of("alg", "HS256", "typ", "JWT")).getBytes(StandardCharsets.UTF_8));
        String body = base64Url(jsonProvider.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + body;
        return signingInput + "." + base64Url(sign(signingInput));
    }

    private JsonProviderImpl jsonProvider() {
        return new JsonProviderImpl(new ObjectMapper().findAndRegisterModules());
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
