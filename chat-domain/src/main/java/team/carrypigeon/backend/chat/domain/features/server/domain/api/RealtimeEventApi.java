package team.carrypigeon.backend.chat.domain.features.server.domain.api;

import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;

/**
 * 实时事件 API。
 * 职责：向 server 内部领域事件映射器暴露 realtime 事件缓存与在线投递能力。
 * 边界：调用方负责把已发生的业务事件映射为稳定 payload，发布实现不依赖生产者内部模型。
 * 输入：包含事件类型、稳定 payload、候选接收账号和通知过滤策略的发布命令。
 * 输出：无返回值，副作用为缓存事件并向符合条件的在线会话投递 frame。
 * 失败语义：事件序列化或投递失败按 server 内部问题处理，不改变业务 feature 的领域模型。
 * 调用方：server 内部的 channel/message 领域事件监听器，不由业务 feature 直接调用。
 */
public interface RealtimeEventApi {

    /**
     * 缓存并向符合条件的在线账号发布 realtime 事件。
     *
     * @param command realtime 事件发布命令
     */
    void publish(PublishRealtimeEventCommand command);
}
