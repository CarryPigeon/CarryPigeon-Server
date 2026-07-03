package team.carrypigeon.backend.chat.domain.features.message.domain.api;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessagePluginCatalogItemResult;

/**
 * 消息插件目录领域 API。
 * 职责：暴露公开插件目录和扩展消息类型支持查询能力。
 * 边界：不暴露 controller 协议、具体 registry 实现和插件实例创建细节。
 * 输入：公开插件目录无入参；扩展类型支持查询使用消息类型标识。
 * 输出：公开插件目录投影或是否支持扩展消息类型的判断。
 * 失败语义：非法消息类型由领域规则处理，调用方不应依赖 registry 异常细节。
 * 调用方：通过本接口展示和判断插件能力，不直接访问插件注册表。
 */
public interface MessagePluginCatalogApi {

    /**
     * 查询公开可见的消息插件目录。
     * 输出：公开插件目录项投影列表。
     * 约束：内部插件、不可用插件或非公开插件不应泄漏给调用方。
     *
     * @return 公开消息插件目录项列表
     */
    List<MessagePluginCatalogItemResult> listPublicPlugins();

    /**
     * 判断是否支持指定扩展消息类型。
     * 输入：扩展消息类型标识。
     * 输出：支持时返回 true，否则返回 false。
     * 边界：只表达能力判断，不创建消息、不执行插件。
     *
     * @param messageType 扩展消息类型标识
     * @return 是否支持该扩展消息类型
     */
    boolean supportsExtensionMessageType(String messageType);
}
