package team.carrypigeon.backend.api.bo.domain.user;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public enum CPUserSexEnum {
    UNKNOWN(0),
    MALE(1),
    FEMALE(2);
    private final int value;
    CPUserSexEnum(int value) {
        this.value = value;
    }

    public static CPUserSexEnum valueOf(int value) {
        return switch (value) {
            case 0 -> UNKNOWN;
            case 1 -> MALE;
            case 2 -> FEMALE;
            default -> throw new IllegalArgumentException("User sex parse:Invalid value: " + value);
        };
    }
}
