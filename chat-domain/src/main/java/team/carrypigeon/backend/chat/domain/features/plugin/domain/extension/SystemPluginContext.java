package team.carrypigeon.backend.chat.domain.features.plugin.domain.extension;

import java.util.Objects;
import org.springframework.context.ApplicationContext;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifest;

/**
 * 系统插件启动上下文。
 * 职责：把主 Spring Context 和当前插件 Manifest 传给插件生命周期。
 * 边界：SYSTEM 插件仍可直接注入 ApplicationContext；此上下文只是统一的生命周期入口。
 */
public record SystemPluginContext(
        ApplicationContext applicationContext,
        PluginManifest manifest
) {

    public SystemPluginContext {
        applicationContext = Objects.requireNonNull(applicationContext, "applicationContext must not be null");
        manifest = Objects.requireNonNull(manifest, "manifest must not be null");
    }

    /**
     * 获取宿主或插件 Bean。
     *
     * @param type Bean 类型
     * @param <T> Bean 类型
     * @return 当前 Context 中的 Bean
     */
    public <T> T requireBean(Class<T> type) {
        return applicationContext.getBean(type);
    }
}
