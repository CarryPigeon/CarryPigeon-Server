package team.carrypigeon.backend.infrastructure.service.storage.impl.health;

import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealth;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;

/** 启动时检查Storage实现的可用性；检查失败会阻止应用启动。 */
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
