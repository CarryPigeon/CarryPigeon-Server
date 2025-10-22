package team.carrypigeon.backend.api.bo.domain.channel.application;

import lombok.Getter;

@Getter
public enum CPChannelApplicationStateEnum {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);
    private final int value;
    CPChannelApplicationStateEnum(int value) {
        this.value = value;
    }
    public static CPChannelApplicationStateEnum valueOf(int value) {
        return switch (value) {
            case 0 -> PENDING;
            case 1 -> APPROVED;
            case 2 -> REJECTED;
            default -> throw new IllegalArgumentException("Channel application state parse:Invalid value: " + value);
        };
    }

}
