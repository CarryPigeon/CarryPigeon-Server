package team.carrypigeon.backend.infrastructure.service.storage.impl.health;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealth;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;

/** 启动时检查Storage实现的可用性；检查失败会阻止应用启动。 */
@EnvironmentComponent(profiles = {PROD, DEV})
public final class MinioInitializationChecker implements InitializationChecker {

    private final StorageHealthService healthService;

    public MinioInitializationChecker(StorageHealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public String name() {
        return "storage";
    }

    @Override
    public InitializationCheckResult check() {
        StorageHealth health = healthService.check();
        return new InitializationCheckResult(health.available(), health.message());
    }
}
