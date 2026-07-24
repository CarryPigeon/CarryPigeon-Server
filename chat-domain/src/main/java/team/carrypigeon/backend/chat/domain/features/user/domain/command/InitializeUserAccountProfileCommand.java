package team.carrypigeon.backend.chat.domain.features.user.domain.command;

import java.time.Instant;

/**
 * 新账号用户资料初始化命令。
 *
 * @param accountId 账号 ID
 * @param nickname 初始昵称
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record InitializeUserAccountProfileCommand(
        long accountId,
        String nickname,
        Instant createdAt,
        Instant updatedAt
) {
}
