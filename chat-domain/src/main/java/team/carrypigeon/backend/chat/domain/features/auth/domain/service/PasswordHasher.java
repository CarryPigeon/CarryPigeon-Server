package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

/**
 * 密码哈希服务抽象。
 * 职责：为鉴权领域提供密码摘要生成与校验能力。
 * 边界：这里只定义哈希语义，不暴露底层密码学实现配置。
 */
public interface PasswordHasher {

    /**
     * 生成密码摘要。
     *
     * @param rawPassword 明文密码
     * @return 哈希后的密码摘要
     */
    String hash(String rawPassword);

    /**
     * 校验明文密码是否匹配既有摘要。
     *
     * @param rawPassword 明文密码
     * @param passwordHash 已保存的密码摘要
     * @return 匹配时返回 true
     */
    boolean matches(String rawPassword, String passwordHash);
}
