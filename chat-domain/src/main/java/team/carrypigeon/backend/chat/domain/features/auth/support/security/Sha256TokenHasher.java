package team.carrypigeon.backend.chat.domain.features.auth.support.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.TokenHasher;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * SHA-256 令牌摘要实现。
 * 职责：为 refresh token 入库前生成稳定摘要，避免保存原始令牌。
 * 边界：不用于密码哈希，不替代 Argon2 密码处理。
 */
@Component
public class Sha256TokenHasher implements TokenHasher {

    @Override
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw ProblemException.fail("token_hash_failed", "failed to hash token");
        }
    }
}
