package team.carrypigeon.backend.chat.domain.features.plugin.domain.projection;

import java.util.List;

/**
 * 消息插件目录项结果。
 * 职责：对外暴露消息插件目录所需的最小只读视图。
 * 边界：这里只表达目录信息，不承载插件运行时实例或发送逻辑。
 *
 * @param messageType 插件对应的内部消息类型
 * @param domain 插件负责的对外 canonical 消息 domain
 * @param publicPluginKey 对外公开插件标识
 * @param description 插件描述
 * @param declaredPermissions 插件声明权限
 */
public record PluginCatalogItemResult(
        String messageType,
        String domain,
        String publicPluginKey,
        String description,
        List<String> declaredPermissions
) {
}
