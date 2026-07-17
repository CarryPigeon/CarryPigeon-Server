package team.carrypigeon.backend.chat.domain.features.auth.support.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.port.PasswordHasher;

/**
 * Argon2 密码哈希实现。
 * 职责：使用 Argon2id 生成与校验密码摘要，满足当前注册与登录阶段的密码处理要求。
 * 边界：不在此处承载登录用例编排与密码策略扩展。
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    /**
     * 对原始密码执行 Argon2id 摘要。
     *
     * @param rawPassword 原始密码
     * @return 可持久化的密码摘要
     */
    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验原始密码与已有摘要是否匹配。
     *
     * @param rawPassword 原始密码
     * @param passwordHash 已保存的密码摘要
     * @return 两者是否匹配
     */
    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
