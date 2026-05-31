package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

/**
 * 邮箱验证码服务抽象。
 * 职责：为邮箱验证码登录流程提供最小签发与校验能力。
 * 边界：这里只定义验证码语义，不承载真实邮件发送基础设施。
 */
public interface EmailVerificationCodeService {

    /**
     * 为目标邮箱签发验证码。
     *
     * @param email 目标邮箱
     */
    void issueCode(String email);

    /**
     * 校验目标邮箱验证码。
     *
     * @param email 目标邮箱
     * @param code 待校验验证码
     */
    void verifyCode(String email, String code);
}
