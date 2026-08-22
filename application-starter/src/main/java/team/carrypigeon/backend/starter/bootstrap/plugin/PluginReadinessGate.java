package team.carrypigeon.backend.starter.bootstrap.plugin;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 插件启动就绪门禁。
 * 职责：表达当前 JVM 是否已完成启动期插件初始化。
 * 边界：该状态供宿主流量入口使用，不提供插件安全隔离能力。
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
