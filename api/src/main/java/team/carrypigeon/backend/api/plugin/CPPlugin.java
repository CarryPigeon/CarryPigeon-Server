package team.carrypigeon.backend.api.plugin;

/**
 * 后端插件统一接口。
 * <p>
 * 外部插件通过实现该接口接入系统，并由 `chat-domain` 中的插件管理器统一加载与初始化。
 */
public interface CPPlugin {

    /**
     * 获取插件名称。
     *
     * @return 人类可读插件名
     */
    String getName();

    /**
     * 获取插件版本。
     *
     * @return 语义化版本字符串
     */
    String getVersion();

    /**
     * 应用启动后执行插件初始化。
     */
    void init();
}
