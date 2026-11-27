package team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification;

/** 邮箱验证码授权的固定业务用途，不同用途之间不能互相消费。 */
public enum EmailCodePurpose {
    REGISTER,
    LOGIN,
    CHANGE_EMAIL,
    RESET_PASSWORD;
}
