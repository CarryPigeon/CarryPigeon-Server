package team.carrypigeon.backend.infrastructure.basic.plugin.message.api;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * 消息处理器
 * 职责：定义插件自身的
 * 边界：这里只负责单个消息类型的解析和中间处理而不关注消息的具体处理
 * */
public interface CPChannelMessageHandler {
    /**
     * 处理的具体逻辑
     * @param message 待处理的消息
     * @return 校验和转换完成的有效消息，不能返回 null；消息身份、频道及事务序号必须保持不变
     * @throws ProblemException 消息校验失败抛出的异常
     * */
    CPChannelMessage handle(CPChannelMessage message) throws ProblemException;
}
