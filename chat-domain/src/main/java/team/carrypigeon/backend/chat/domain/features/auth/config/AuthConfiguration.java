package team.carrypigeon.backend.chat.domain.features.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenSettings;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.auth.support.verification.CacheBackedEmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.auth.support.verification.InMemoryEmailVerificationCodeService;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

/**
 * 鉴权功能配置入口。
 * 职责：启用 auth feature 内部的稳定配置绑定。
 * 边界：不创建外部服务实现 Bean，不承载鉴权业务规则。
 */
@Configuration
@EnableConfigurationProperties(AuthJwtProperties.class)
public class AuthConfiguration {

    /**
     * 创建领域侧 token 签发设置。
     * 职责：把配置绑定对象转换为领域服务需要的最小时间参数。
     *
     * @param properties JWT 配置绑定对象
     * @return token 签发设置
     */
    @Bean
    public AuthTokenSettings authTokenSettings(AuthJwtProperties properties) {
        return new AuthTokenSettings(properties.accessTokenTtl(), properties.refreshTokenTtl());
    }

    /**
     * 创建邮箱验证码服务。
     * 职责：优先使用共享缓存实现，缺失缓存 Bean 时退回到最小内存实现以保持测试上下文可装配。
     *
     * @param cacheServiceProvider 缓存服务提供者
     * @param timeProvider 时间提供者
     * @return 邮箱验证码服务
     */
    @Bean
    public EmailVerificationCodeService emailVerificationCodeService(
            ObjectProvider<CacheService> cacheServiceProvider,
            ObjectProvider<MailSenderService> mailSenderServiceProvider,
            TimeProvider timeProvider
    ) {
        CacheService cacheService = cacheServiceProvider.getIfAvailable();
        MailSenderService mailSenderService = mailSenderServiceProvider.getIfAvailable();
        if (cacheService != null) {
            return new CacheBackedEmailVerificationCodeService(cacheService, mailSenderService);
        }
        return new InMemoryEmailVerificationCodeService(timeProvider, mailSenderService);
    }
}
