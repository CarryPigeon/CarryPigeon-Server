package team.carrypigeon.backend.infrastructure.service.cache.impl.startup;

import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealth;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;

/**
 * 缓存初始化检查。
 * 职责：将缓存健康检查适配为共享启动检查契约。
 * 边界：只负责契约转换，不暴露 Redis 连接细节。
 */
public class CacheInitializationCheck implements InitializationCheck {

    private final CacheHealthService cacheHealthService;

    public CacheInitializationCheck(CacheHealthService cacheHealthService) {
        this.cacheHealthService = cacheHealthService;
    }

    @Override
    public String name() {
        return "cache";
    }

    @Override
    public InitializationCheckResult check() {
        CacheHealth health = cacheHealthService.check();
        return health.available()
                ? InitializationCheckResult.passed(health.message())
                : InitializationCheckResult.failed(health.message());
    }
}
