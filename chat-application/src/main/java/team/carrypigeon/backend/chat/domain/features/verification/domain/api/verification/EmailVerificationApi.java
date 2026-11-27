package team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification;

/**
 * 邮箱验证领域 API。
 * 职责：暴露邮箱验证码签发与校验能力。
 */
public interface EmailVerificationApi {

    /**
     * 签发验证码并受理异步邮件任务；正常返回不保证 SMTP 已受理或收件箱已收到。
     *
     * @param email 签发验证码的邮箱
     * @param purpose 验证码授权用途，不能为空
     * @param accountId 修改邮箱时为当前认证账号 ID；注册和登录时必须为 null
     * @throws team.carrypigeon.backend.infrastructure.basic.exception.ProblemException 用途不合法、缺少认证或投递失败
     */
    void issueCode(String email, EmailCodePurpose purpose, Long accountId);

    /**
     * 校验并消费邮箱验证码。
     * @param email 邮箱
     * @param code 邮箱验证码
     * @param purpose 由调用业务固定指定，不能使用客户端提交的用途代替
     * @param accountId 修改邮箱时为被修改账号 ID；注册和登录时必须为 null
     */
    void verifyCode(String email, String code, EmailCodePurpose purpose, Long accountId);
}
