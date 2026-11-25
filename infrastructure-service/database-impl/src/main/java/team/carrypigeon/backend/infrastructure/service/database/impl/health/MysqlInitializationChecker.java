package team.carrypigeon.backend.infrastructure.service.database.impl.health;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealth;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;

/** 启动时检查Database实现的可用性；检查失败会阻止应用启动。 */
@EnvironmentComponent(profiles = {PROD, DEV})
public final class MysqlInitializationChecker implements InitializationChecker {

    private final DatabaseHealthService healthService;

    public MysqlInitializationChecker(DatabaseHealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public String name() {
        return "database";
    }

    @Override
    public InitializationCheckResult check() {
        DatabaseHealth health = healthService.check();
        return new InitializationCheckResult(health.available(), health.message());
    }
}
