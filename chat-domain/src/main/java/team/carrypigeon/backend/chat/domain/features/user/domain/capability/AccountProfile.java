package team.carrypigeon.backend.chat.domain.features.user.domain.capability;

import java.time.Instant;

/**
 * 用户信息account层的数据映射
 * */
public record AccountProfile(
    long userId,
    String username,
    String email,
    Instant createdAt
) {
}
