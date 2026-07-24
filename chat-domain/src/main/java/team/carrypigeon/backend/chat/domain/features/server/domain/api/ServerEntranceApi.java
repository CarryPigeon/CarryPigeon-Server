package team.carrypigeon.backend.chat.domain.features.server.domain.api;

import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerDiscoveryDocument;

/**
 * 服务入口领域 API。
 * 职责：暴露服务发现文档。
 * 边界：不暴露 controller 协议、服务内部运行时配置和具体业务资源能力。
 * 输入：服务发现无入参。
 * 输出：服务发现文档。
 * 失败语义：插件 ID 列表非法等问题由领域问题异常表达。
 * 调用方：启动页、网关或 controller 通过本接口读取服务入口能力，不直接读取配置对象。
 */
public interface ServerEntranceApi {

    /**
     * 获取服务发现文档。
     * 输出：描述当前服务身份、能力和入口约束的发现文档投影。
     * 边界：该结果面向领域入口语义，不等同于运行时配置原始对象。
     *
     * @return 服务发现文档投影
     */
    ServerDiscoveryDocument getServerDiscoveryDocument();

}
