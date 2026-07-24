package team.carrypigeon.backend.chat.domain.features.verification.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.verification.domain.capability.EmailVerificationCapability;
import team.carrypigeon.backend.chat.domain.features.verification.support.CacheBackedEmailVerificationCapability;
import team.carrypigeon.backend.chat.domain.features.verification.support.InMemoryEmailVerificationCapability;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/**
 * 邮箱验证 feature 配置。
 * 职责：选择共享缓存或进程内验证码 capability，并装配可选邮件投递能力。
 * 边界：不创建 cache/mail 的具体基础设施实现。
 */
@Configuration
public class EmailVerificationConfiguration {

    /**
     * 创建邮箱验证码内部 capability。
     *
     * @param cacheServiceProvider 缓存服务提供器
     * @param mailSenderServiceProvider 邮件发送服务提供器
     * @param timeProvider 时间提供器
     * @return 邮箱验证码内部 capability
     */
    @Bean
    public EmailVerificationCapability emailVerificationCapability(
            ObjectProvider<CacheService> cacheServiceProvider,
            ObjectProvider<MailSenderService> mailSenderServiceProvider,
            TimeProvider timeProvider
    ) {
        CacheService cacheService = cacheServiceProvider.getIfAvailable();
        MailSenderService mailSenderService = mailSenderServiceProvider.getIfAvailable();
        if (cacheService != null) {
            return new CacheBackedEmailVerificationCapability(cacheService, mailSenderService);
        }
        return new InMemoryEmailVerificationCapability(timeProvider, mailSenderService);
    }
}
