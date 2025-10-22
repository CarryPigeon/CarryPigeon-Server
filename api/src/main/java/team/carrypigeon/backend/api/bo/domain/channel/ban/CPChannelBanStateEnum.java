package team.carrypigeon.backend.api.bo.domain.channel.ban;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CPChannelBanStateEnum {
    VALID(1),INVALID(2);
    private final int state;

    public static CPChannelBanStateEnum valueOf(int state) {
        return switch (state) {
            case 1 -> VALID;
            case 2 -> INVALID;
            default -> throw new IllegalArgumentException("Channel Ban State parse:Invalid state: " + state);
        };
    }

}
