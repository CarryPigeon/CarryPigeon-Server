package team.carrypigeon.backend.api.bo.domain.user;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户性别枚举。
 */
@Getter
@Slf4j
public enum CPUserSexEnum {

    /**
     * 未知。
     */
    UNKNOWN(0),

    /**
     * 男。
     */
    MALE(1),

    /**
     * 女。
     */
    FEMALE(2);

    private final int value;

    /**
     * 构造性别枚举。
     *
     * @param value 枚举持久化值。
     */
    CPUserSexEnum(int value) {
        this.value = value;
    }

    /**
     * 按持久化值解析性别。
     *
     * @param value 持久化值。
     * @return 性别枚举。
     * @throws IllegalArgumentException 当值非法时抛出。
     */
    public static CPUserSexEnum valueOf(int value) {
        return switch (value) {
            case 0 -> UNKNOWN;
            case 1 -> MALE;
            case 2 -> FEMALE;
            default -> {
                log.error("User sex parse failed, invalid value: {}", value);
                throw new IllegalArgumentException("User sex parse:Invalid value: " + value);
            }
        };
    }
}
