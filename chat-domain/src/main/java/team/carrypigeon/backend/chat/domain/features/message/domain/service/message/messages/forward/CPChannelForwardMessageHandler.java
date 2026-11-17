package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.forward;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageHandler;

/**
 * 转发消息处理器占位，尚未实现载荷校验与转换，不参与消息能力注册。
 * 实现完整发送契约后才能添加 MessageHandler 注解。
 */
public class CPChannelForwardMessageHandler implements CPChannelMessageHandler {
    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        throw ProblemException.of(MessageProblem.UNSUPPORTED_DOMAIN);
    }
}
