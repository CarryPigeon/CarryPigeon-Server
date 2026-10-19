package team.carrypigeon.backend.infrastructure.service.database.impl.startup;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealth;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;

/**
 * 数据库初始化检查。
 * 职责：将数据库健康检查适配为共享启动检查契约。
 * 边界：只负责契约转换，不承载 JDBC 检查细节。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseInitializationChecker implements InitializationChecker {

    private final DatabaseHealthService databaseHealthService;

    public DatabaseInitializationChecker(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }

    /**
     * 返回启动检查项名称。
     */
    @Override
    public String name() {
        return "database";
    }

    /**
     * 执行数据库健康检查并转换为统一启动检查结果。
     */
    @Override
    public InitializationCheckResult check() {
        DatabaseHealth health = databaseHealthService.check();
        return health.available()
                ? InitializationCheckResult.passed(health.message())
                : InitializationCheckResult.failed(health.message());
    }
}
