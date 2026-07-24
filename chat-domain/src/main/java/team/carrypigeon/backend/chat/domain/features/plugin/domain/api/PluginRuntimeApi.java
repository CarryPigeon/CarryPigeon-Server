package team.carrypigeon.backend.chat.domain.features.plugin.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.PluginRuntimeStatusResult;

/**
 * 插件运行时领域 API。
 *
 * <p>职责：由启动装配调用插件的启动、停止和状态查询。边界：不提供运行期加载、卸载或替换能力。</p>
 */
public interface PluginRuntimeApi {

    void start();

    void stop();

    List<PluginRuntimeStatusResult> statuses();
}
