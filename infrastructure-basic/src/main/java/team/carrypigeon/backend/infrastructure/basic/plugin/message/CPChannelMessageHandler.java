package team.carrypigeon.backend.infrastructure.basic.plugin.message;

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
     * @return 处理后的消息
     * @throws ProblemException 消息校验失败抛出的异常
     * */
    CPChannelMessage handle(CPChannelMessage message) throws ProblemException;
}
