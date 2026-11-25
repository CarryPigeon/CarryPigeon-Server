package team.carrypigeon.backend.infrastructure.service.cache.impl.health;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealth;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;

/** 启动时检查Cache实现的可用性；检查失败会阻止应用启动。 */
@EnvironmentComponent(profiles = {PROD, DEV})
public final class RedisInitializationChecker implements InitializationChecker {

    private final CacheHealthService healthService;

    public RedisInitializationChecker(CacheHealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public String name() {
        return "cache";
    }

    @Override
    public InitializationCheckResult check() {
        CacheHealth health = healthService.check();
        return new InitializationCheckResult(health.available(), health.message());
    }
}
