package team.carrypigeon.backend.api.bo.domain.channel.application;

import lombok.Getter;

@Getter
/**
 * 频道申请状态枚举。
 * <p>
 * 注意：为了兼容历史数据，{@link #valueOf(int)} 在遇到非法值时会返回 {@code null}。
 * </p>
 */
public enum CPChannelApplicationStateEnum {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);
    private final int value;
    CPChannelApplicationStateEnum(int value) {
        this.value = value;
    }

    /**
     * 将数值转换为状态枚举；非法值返回 {@code null}。
     *
     * @param value 持久化/传输的状态数值
     * @return 对应状态枚举；非法值返回 {@code null}
     */
    public static CPChannelApplicationStateEnum valueOf(int value) {
        return switch (value) {
            case 0 -> PENDING;
            case 1 -> APPROVED;
            case 2 -> REJECTED;
            default -> null;
        };
    }

}
