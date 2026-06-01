package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 文本频道消息插件。
 * 职责：把 text 消息草稿转换为当前持久化与实时链路兼容的领域消息。
 * 边界：只处理文本消息，不扩展其它消息类型规则。
 */
public class TextChannelMessagePlugin implements ChannelMessagePlugin {

    /**
     * 返回当前插件负责的消息类型。
     *
     * @return `text` 消息类型标识
     */
    @Override
    public String supportedType() {
        return "text";
    }

    /**
     * 校验文本草稿并构造文本消息。
     * 输入：消息构建上下文与文本草稿。
     * 约束：正文不能为空白。
     * 输出：可持久化的文本消息领域对象。
     *
     * @param context 消息构建上下文
     * @param draft 入站消息草稿
     * @return 文本消息领域对象
     */
    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof TextChannelMessageDraft textDraft)) {
            throw new IllegalArgumentException("text plugin only supports TextChannelMessageDraft");
        }
        if (textDraft.body() == null || textDraft.body().isBlank()) {
            throw ProblemException.validationFailed("body must not be blank");
        }
        String normalizedBody = textDraft.body().trim();
        return new ChannelMessage(
                context.messageId(),
                context.serverId(),
                context.conversationId(),
                context.channelId(),
                context.senderId(),
                supportedType(),
                normalizedBody,
                normalizedBody,
                normalizedBody,
                textDraft.payload(),
                textDraft.metadata(),
                null,
                null,
                "sent",
                context.createdAt(),
                null,
                1L
        );
    }
}
