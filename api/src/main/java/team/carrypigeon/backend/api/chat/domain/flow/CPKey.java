package team.carrypigeon.backend.api.chat.domain.flow;

import java.util.Objects;

/**
 * 强类型上下文 Key。
 * <p>
 * LiteFlow {@link CPFlowContext} 本质仍是 {@code Map<String, Object>}，
 * 该 Key 用于把“名称 + 类型”绑定在一起，提升可读性并减少类型误用。
 *
 * @param name Key 名称（必须与链路约定一致）
 * @param type 值类型（运行时校验使用）
 * @param <T>  值类型
 */
public record CPKey<T>(String name, Class<T> type) {

    public CPKey {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public static <T> CPKey<T> of(String name, Class<T> type) {
        return new CPKey<>(name, type);
    }
}

