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

    @Override
    public String supportedType() {
        return "text";
    }

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
                "sent",
                context.createdAt()
        );
    }
}
