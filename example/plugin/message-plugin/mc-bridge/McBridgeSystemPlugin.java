package example.plugin.messageplugin.mcbridge;

import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.PluginHealth;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.SystemPlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.SystemPluginContext;

/**
 * mc-bridge 系统插件生命周期示例。
 *
 * <p>该插件运行在宿主主 Spring Context 中；需要数据库或其它核心能力时可在此类或其它 Bean 中直接构造器注入。</p>
 */
public class McBridgeSystemPlugin implements SystemPlugin {

    @Override
    public String pluginId() {
        return "com-example-mc-bridge";
    }

    @Override
    public void start(SystemPluginContext context) {
        // 示例插件没有外部连接需要初始化；真实插件可在这里读取 context.requireBean(...).
    }

    @Override
    public PluginHealth health() {
        return PluginHealth.active();
    }
}
