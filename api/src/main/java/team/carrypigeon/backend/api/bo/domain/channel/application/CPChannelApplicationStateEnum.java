package team.carrypigeon.backend.api.bo.domain.channel.application;

import lombok.Getter;

/**
 * 频道申请状态枚举。
 */
@Getter
public enum CPChannelApplicationStateEnum {

    /**
     * 待处理。
     */
    PENDING(0),

    /**
     * 已通过。
     */
    APPROVED(1),

    /**
     * 已拒绝。
     */
    REJECTED(2);

    private final int value;

    /**
     * 构造申请状态枚举。
     *
     * @param value 枚举持久化值。
     */
    CPChannelApplicationStateEnum(int value) {
        this.value = value;
    }

    /**
     * 按持久化值解析申请状态。
     *
     * @param value 持久化值。
     * @return 对应状态；非法值返回 {@code null}。
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
