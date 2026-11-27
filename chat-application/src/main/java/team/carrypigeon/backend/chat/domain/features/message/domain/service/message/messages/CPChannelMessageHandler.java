package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 消息处理器
 * 职责：校验 domain 载荷并派生完整消息数据、展示预览和 searchText 正文。
 * 边界：searchText 只包含本消息可检索内容，不加入回复快照等元数据；不负责持久化或发送通知。
 * */
public interface CPChannelMessageHandler {
    /**
     * 处理的具体逻辑
     * @param message 待处理的消息
     * @return 校验和转换完成的有效消息，不能返回 null；必须保留消息 ID 和所属频道
     * @throws ProblemException 消息校验失败抛出的异常
     * */
    CPChannelMessage handle(CPChannelMessage message) throws ProblemException;
}
