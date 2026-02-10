package team.carrypigeon.backend.api.chat.domain.flow;

import java.util.Objects;

/**
 * 强类型上下文键。
 *
 * @param name 键名。
 * @param type 值类型。
 * @param <T> 值类型参数。
 */
public record CPKey<T>(String name, Class<T> type) {

    /**
     * 校验键名与类型合法性。
     */
    public CPKey {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /**
     * 创建强类型上下文键。
     *
     * @param name 键名。
     * @param type 值类型。
     * @param <T> 值类型参数。
     * @return 强类型上下文键。
     */
    public static <T> CPKey<T> of(String name, Class<T> type) {
        return new CPKey<>(name, type);
    }
}
