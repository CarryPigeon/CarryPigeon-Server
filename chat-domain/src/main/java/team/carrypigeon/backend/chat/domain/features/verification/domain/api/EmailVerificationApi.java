package team.carrypigeon.backend.chat.domain.features.verification.domain.api;

import team.carrypigeon.backend.chat.domain.features.verification.domain.command.IssueEmailVerificationCodeCommand;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand;

/**
 * 邮箱验证领域 API。
 * 职责：向 auth、user 和协议入口暴露邮箱验证码签发与校验能力。
 * 边界：不暴露缓存、邮件客户端或验证码存储实现。
 * 输入：邮箱验证码签发或校验命令。
 * 输出：无返回值，副作用为签发投递或校验消费验证码。
 * 失败语义：邮箱、验证码、投递和过期问题由领域问题异常表达。
 * 调用方：auth、user 与 verification controller 只能依赖本接口，不直接依赖内部 capability。
 */
public interface EmailVerificationApi {

    /**
     * 签发并投递邮箱验证码。
     * 副作用：生成验证码并委托邮件能力投递。
     *
     * @param command 验证码签发命令
     */
    void issueCode(IssueEmailVerificationCodeCommand command);

    /**
     * 校验并消费邮箱验证码。
     * 副作用：校验成功后消费一次性验证码。
     *
     * @param command 验证码校验命令
     */
    void verifyCode(VerifyEmailVerificationCodeCommand command);
}
