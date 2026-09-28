package team.carrypigeon.backend.chat.domain.features.auth.domain.model;

import java.time.Instant;

/**
 * 对外公开的account元信息
 * */
public record AuthPublicAccount(
        long id,
        String username,
        String email,
        Instant createdAt
) {
}
