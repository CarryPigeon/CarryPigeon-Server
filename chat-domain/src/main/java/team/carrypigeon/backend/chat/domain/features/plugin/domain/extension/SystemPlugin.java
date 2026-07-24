package team.carrypigeon.backend.chat.domain.features.plugin.domain.extension;

/**
 * 启动期全权限系统插件契约。
 * 职责：提供插件启动、健康检查和停止生命周期。
 * 边界：SYSTEM 插件可以通过主 Spring Context 直接访问宿主资源；本接口不是安全隔离边界。
 */
public interface SystemPlugin {

    /**
     * 返回与 Manifest 一致的插件 ID。
     *
     * @return 插件唯一标识
     */
    String pluginId();

    /**
     * 启动插件。
     *
     * @param context 主 Spring Context 和已校验 Manifest
     */
    void start(SystemPluginContext context);

    /**
     * 返回当前插件健康状态。
     *
     * @return 插件健康状态
     */
    default PluginHealth health() {
        return PluginHealth.active();
    }

    /**
     * 停止插件并释放插件创建的资源。
     * 启动回调发生部分失败时也可能被调用，实现必须允许幂等、尽力清理。
     */
    default void stop() {
        // 默认插件没有额外停止动作。
    }
}
