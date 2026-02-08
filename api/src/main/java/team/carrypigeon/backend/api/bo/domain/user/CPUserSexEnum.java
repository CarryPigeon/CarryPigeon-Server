package team.carrypigeon.backend.api.bo.domain.user;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
/**
 * 用户性别枚举（用于用户资料展示）。
 */
public enum CPUserSexEnum {
    UNKNOWN(0),
    MALE(1),
    FEMALE(2);
    private final int value;
    CPUserSexEnum(int value) {
        this.value = value;
    }

    /**
     * 将数值转换为枚举。
     *
     * @param value 持久化/传输的枚举数值
     * @return 对应的性别枚举
     * @throws IllegalArgumentException 当 value 不合法时抛出
     */
    public static CPUserSexEnum valueOf(int value) {
        return switch (value) {
            case 0 -> UNKNOWN;
            case 1 -> MALE;
            case 2 -> FEMALE;
            default -> {
                // 记录非法枚举值，便于排查调用方错误
                log.error("User sex parse failed, invalid value: {}", value);
                throw new IllegalArgumentException("User sex parse:Invalid value: " + value);
            }
        };
    }
}
