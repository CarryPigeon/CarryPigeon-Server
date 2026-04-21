package team.carrypigeon.backend.infrastructure.service.database.impl.startup;

import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealth;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;

/**
 * 数据库初始化检查。
 * 职责：将数据库健康检查适配为共享启动检查契约。
 * 边界：只负责契约转换，不承载 JDBC 检查细节。
 */
public class DatabaseInitializationCheck implements InitializationCheck {

    private final DatabaseHealthService databaseHealthService;

    public DatabaseInitializationCheck(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }

    @Override
    public String name() {
        return "database";
    }

    @Override
    public InitializationCheckResult check() {
        DatabaseHealth health = databaseHealthService.check();
        return health.available()
                ? InitializationCheckResult.passed(health.message())
                : InitializationCheckResult.failed(health.message());
    }
}
