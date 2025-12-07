package team.carrypigeon.backend.api.bo.domain.channel.member;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public enum CPChannelMemberAuthorityEnum {
    MEMBER(0),
    ADMIN(1),;
    private final int authority;
    CPChannelMemberAuthorityEnum(int authority) {
        this.authority = authority;
    }

    public static CPChannelMemberAuthorityEnum valueOf(int authority) {
        return switch (authority) {
            case 0 -> MEMBER;
            case 1 -> ADMIN;
            default -> {
                // 记录非法权限值，便于排查调用方错误
                log.error("Channel member authority parse failed, invalid authority: {}", authority);
                throw new IllegalArgumentException("Channel member authority parse:Invalid authority: " + authority);
            }
        };
    }
}
