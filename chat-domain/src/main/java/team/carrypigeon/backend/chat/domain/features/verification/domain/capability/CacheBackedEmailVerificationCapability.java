package team.carrypigeon.backend.chat.domain.features.verification.domain.capability;

import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;

/**
 * 邮箱验证码 feature 内部能力抽象。
 * 职责：按用途及账号绑定存储、投递和消费邮箱挑战；入参已由领域服务校验并规范化。
 * 边界：这里只定义验证码语义，不暴露真实邮件基础设施实现细节。
 */
public interface CacheBackedEmailVerificationCapability {

    /**
     * 为目标邮箱签发验证码。
     *
     * @param email 目标邮箱
     * @param purpose 固定业务用途
     * @param accountId 修改邮箱的认证账号 ID，其余用途为 null
     */
    void issueCode(String email, EmailCodePurpose purpose, Long accountId);

    /**
     * 校验目标邮箱验证码。
     *
     * @param email 目标邮箱
     * @param code 待校验验证码
     * @param purpose 消费业务的固定用途
     * @param accountId 修改邮箱的认证账号 ID，其余用途为 null
     */
    void verifyCode(String email, String code, EmailCodePurpose purpose, Long accountId);
}
