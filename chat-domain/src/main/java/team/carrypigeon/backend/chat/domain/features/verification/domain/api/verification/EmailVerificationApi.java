package team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification;

/**
 * 邮箱验证领域 API。
 * 职责：暴露邮箱验证码签发与校验能力。
 */
public interface EmailVerificationApi {

    /**
     * 签发并投递邮箱验证码。
     *
     * @param email 签发验证码的邮箱
     */
    void issueCode(String email);

    /**
     * 校验并消费邮箱验证码。
     * @param email 邮箱
     * @param code 邮箱验证吗
     */
    void verifyCode(String email,String code);
}
