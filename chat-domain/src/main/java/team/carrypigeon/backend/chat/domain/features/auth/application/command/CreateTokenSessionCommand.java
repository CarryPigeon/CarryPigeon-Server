package team.carrypigeon.backend.chat.domain.features.auth.application.command;

/**
 * 会话令牌创建命令。
 */
public record CreateTokenSessionCommand(String grantType, String email, String code) {
}
