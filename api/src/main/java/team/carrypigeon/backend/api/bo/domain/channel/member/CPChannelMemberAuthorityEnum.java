package team.carrypigeon.backend.api.bo.domain.channel.member;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 频道成员权限枚举。
 */
@Getter
@Slf4j
public enum CPChannelMemberAuthorityEnum {

    /**
     * 普通成员。
     */
    MEMBER(0),

    /**
     * 管理员。
     */
    ADMIN(1);

    private final int authority;

    /**
     * 构造权限枚举。
     *
     * @param authority 权限持久化值。
     */
    CPChannelMemberAuthorityEnum(int authority) {
        this.authority = authority;
    }

    /**
     * 按持久化值解析权限枚举。
     *
     * @param authority 权限持久化值。
     * @return 权限枚举。
     * @throws IllegalArgumentException 当值非法时抛出。
     */
    public static CPChannelMemberAuthorityEnum valueOf(int authority) {
        return switch (authority) {
            case 0 -> MEMBER;
            case 1 -> ADMIN;
            default -> {
                log.error("Channel member authority parse failed, invalid authority: {}", authority);
                throw new IllegalArgumentException("Channel member authority parse:Invalid authority: " + authority);
            }
        };
    }
}
