package team.carrypigeon.backend.common.id;

import cn.hutool.core.codec.Base64;

/**
 * ID 与简易 token 生成工具。
 * <p>
 * 注意：{@link #generateToken()} 仅用于“轻量级、非安全场景”的标识生成，不等同于安全认证令牌。
 * </p>
 */
public class IdUtil {
    private IdUtil() {
    }

    /**
     * 生成一个全局唯一 ID（Snowflake）。
     */
    public static long generateId() {
        return cn.hutool.core.util.IdUtil.getSnowflake().nextId();
    }

    /**
     * 生成一个 Base64 编码的“可读 token”（本质为 ID 的字符串表示）。
     * <p>
     * 该 token 可逆且不可用于安全认证；如需登录/鉴权，请使用 {@code CPUserToken} 等安全方案。
     * </p>
     */
    public static String generateToken() {
        return Base64.encode(String.valueOf(generateId()));
    }
}
