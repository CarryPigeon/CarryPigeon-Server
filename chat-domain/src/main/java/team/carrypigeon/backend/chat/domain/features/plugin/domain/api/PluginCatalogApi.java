package team.carrypigeon.backend.chat.domain.features.plugin.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.PluginCatalogItemResult;

/**
 * 消息插件目录领域 API。
 * 职责：暴露公开插件目录、required plugin 配置与客户端准入检查能力。
 * 边界：不暴露 controller 协议、具体 registry 实现和插件实例创建细节。
 * 输入：公开插件目录无入参；准入检查使用客户端已安装插件 ID 列表。
 * 输出：公开插件目录、required plugin ID 或缺失插件 ID。
 * 失败语义：非法消息类型由领域规则处理，调用方不应依赖 registry 异常细节。
 * 调用方：通过本接口展示和判断插件能力，不直接访问插件注册表。
 */
public interface PluginCatalogApi {

    /**
     * 查询公开可见的消息插件目录。
     * 输出：公开插件目录项投影列表。
     * 约束：内部插件、不可用插件或非公开插件不应泄漏给调用方。
     *
     * @return 公开消息插件目录项列表
     */
    List<PluginCatalogItemResult> listPublicPlugins();

    /**
     * 返回服务端要求客户端安装的插件 ID。
     *
     * @return 去空白、去重后的 required plugin ID
     */
    List<String> requiredPluginIds();

    /**
     * 返回客户端尚未安装的必需插件。
     *
     * @param installedPluginIds 客户端已安装插件 ID
     * @return 缺失插件 ID
     */
    List<String> findMissingRequiredPlugins(List<String> installedPluginIds);

    /**
     * 校验客户端是否满足必需插件 gate。
     *
     * @param installedPluginIds 客户端已安装插件 ID
     */
    void requireRequiredPluginsSatisfied(List<String> installedPluginIds);
}
