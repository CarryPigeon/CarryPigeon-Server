package team.carrypigeon.backend.chat.domain.features.auth.domain.api.session.input;

/**
 * 会话令牌创建命令。
 */
public record CreateTokenSessionCommand(String grantType, String email, String code) {
}
