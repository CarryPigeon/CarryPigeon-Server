package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import team.carrypigeon.backend.chat.domain.features.message.domain.capability.UserProfileQueryAdapter;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.annotation.MessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPMessageStatus;

/**
 * CarryPigeon 预定义的文本消息处理器<br/>
 * 职责：校验文本载荷及回复目标，构造消息数据和预览<br/>
 * 边界：仅允许回复当前频道内未撤回的消息，不负责持久化、提及或事件通知<br/>
 *
 * 预处理消息格式:{@link CPChannelTextMessageOriginData}<br/>
 * 入库格式：{@link CPChannelTextMessageData}
 * */
@MessageHandler(CPChannelTextMessageData.DOMAIN)
public class CPChannelTextMessageHandler implements CPChannelMessageHandler {
    private static final int MAX_CONTENT_CODE_POINTS = 4096;
    private static final int PREVIEW_CODE_POINTS = 20;

    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    private final UserProfileQueryAdapter userProfileQueryAdapter;

    public CPChannelTextMessageHandler(
        ObjectMapper objectMapper,
        MessageRepository messageRepository, UserProfileQueryAdapter userProfileQueryAdapter
    ) {
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
        this.userProfileQueryAdapter = userProfileQueryAdapter;
    }

    @Override
    public CPChannelMessage handle(CPChannelMessage message) throws ProblemException {
        CPChannelTextMessageOriginData originData = parseAndValidate(message.data());
        String content = originData.content();
        Long replyMessageId = originData.replyToMid();

        // 归属与状态校验通过后才读取发送者资料，避免从无权访问的消息派生快照。
        CPChannelTextMessageData.CPChannelTextMessageReplyToUser replyToUser = null;
        if (replyMessageId != null) {
            CPChannelMessage repliedMessage = messageRepository.findById(replyMessageId)
                .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            // 跨频道和已撤回的回复目标统一按不存在处理，避免泄露目标消息信息。
            if (repliedMessage.channelId() != message.channelId() || repliedMessage.status() != CPMessageStatus.SENT) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            String senderName = userProfileQueryAdapter.findSenderName(repliedMessage.senderId());
            replyToUser = new CPChannelTextMessageData.CPChannelTextMessageReplyToUser(
                senderName,
                repliedMessage.preview(),
                repliedMessage.sendTime().toEpochMilli()
            );
        }

        CPChannelTextMessageData data = new CPChannelTextMessageData(
            content, replyMessageId, replyToUser
        );
        int previewEnd = content.offsetByCodePoints(
            0, Math.min(content.codePointCount(0, content.length()), PREVIEW_CODE_POINTS)
        );
        return new CPChannelMessage(
            message.messageId(),
            message.senderId(),
            message.channelId(),
            message.sequence(),
            message.domain(),
            objectMapper.convertValue(data, new TypeReference<>() {}),
            message.sendTime(),
            message.mentions(),
            content.substring(0, previewEnd),
            message.status()
        );
    }

    private CPChannelTextMessageOriginData parseAndValidate(Map<String, Object> payload) {
        // 直接检查原始 JSON 值，避免 Jackson 把数字或布尔值宽松转换成字符串。
        if (!(payload.get("content") instanceof String content) || content.isBlank()) {
            throw invalidField("data.content", "content must be a non-blank string");
        }
        if (content.codePointCount(0, content.length()) > MAX_CONTENT_CODE_POINTS) {
            throw invalidField("data.content", "content must contain at most 4096 Unicode code points");
        }
        Object replyValue = payload.get("reply_to_mid");
        if (replyValue == null) {
            return new CPChannelTextMessageOriginData(content, null);
        }
        if (!(replyValue instanceof Long replyMessageId) || replyMessageId <= 0) {
            throw invalidField("data.reply_to_mid", "reply_to_mid must be a positive message ID");
        }
        return new CPChannelTextMessageOriginData(content, replyMessageId);
    }

    private ProblemException invalidField(String field, String message) {
        return ProblemException.of(MessageProblem.INVALID_INPUT, message, Map.of(
            "field_errors", List.of(Map.of("field", field, "reason", "invalid", "message", message))
        ));
    }
}
