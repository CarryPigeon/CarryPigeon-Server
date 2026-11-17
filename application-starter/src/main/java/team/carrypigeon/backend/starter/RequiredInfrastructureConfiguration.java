package team.carrypigeon.backend.starter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/** 运行产品要求四类基础设施均可用；关闭默认实现时必须提供可用替代。 */
@Configuration(proxyBeanMethods = false)
public class RequiredInfrastructureConfiguration {
    @Bean
    public InitializationChecker requiredInfrastructureCheck(DatabaseHealthService database,
            CacheHealthService cache, MailHealthService mail, StorageHealthService storage,
            CacheService cacheService, MailSenderService mailService, ObjectStorageService storageService) {
        // 在此强制依赖能力并统一检查替代实现；不能因默认自动配置退出而省略启动检查。
        return new InitializationChecker() {
            @Override public String name() { return "required-infrastructure"; }
            @Override public InitializationCheckResult check() {
                if (!database.check().available()) return InitializationCheckResult.failed("database is unavailable");
                if (!cache.check().available()) return InitializationCheckResult.failed("cache is unavailable");
                if (!mail.check().available()) return InitializationCheckResult.failed("mail is unavailable");
                if (!storage.check().available()) return InitializationCheckResult.failed("storage is unavailable");
                return InitializationCheckResult.passed("all required infrastructure is available");
            }
        };
    }
}
