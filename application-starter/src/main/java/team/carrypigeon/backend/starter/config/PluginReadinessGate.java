package team.carrypigeon.backend.starter.config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 插件启动就绪门禁。
 *
 * <p>职责：在插件迁移、启动回调和健康检查完成前拒绝业务 HTTP 流量。边界：这不是安全隔离，
 * 只表达当前 JVM 是否已经完成启动期插件初始化。</p>
 */
public final class PluginReadinessGate {

    private final AtomicBoolean ready = new AtomicBoolean(false);

    public boolean isReady() {
        return ready.get();
    }

    public void markReady() {
        ready.set(true);
    }

    public void markNotReady() {
        ready.set(false);
    }
}
