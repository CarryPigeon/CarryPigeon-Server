package team.carrypigeon.backend.infrastructure.service.storage.impl.startup;

import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealth;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;

/**
 * 对象存储初始化检查。
 * 职责：将对象存储健康检查适配为共享启动检查契约。
 * 边界：只负责契约转换，不暴露 MinIO SDK 细节。
 */
public class StorageInitializationCheck implements InitializationCheck {

    private final StorageHealthService storageHealthService;

    public StorageInitializationCheck(StorageHealthService storageHealthService) {
        this.storageHealthService = storageHealthService;
    }

    @Override
    public String name() {
        return "storage";
    }

    @Override
    public InitializationCheckResult check() {
        StorageHealth health = storageHealthService.check();
        return health.available()
                ? InitializationCheckResult.passed(health.message())
                : InitializationCheckResult.failed(health.message());
    }
}
