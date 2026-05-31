package team.carrypigeon.backend.chat.domain.features.message.application.command;

import java.util.List;
import java.util.Map;

/**
 * HTTP 发送频道消息命令。
 */
public record SendChannelMessageHttpCommand(
        long accountId,
        long channelId,
        String domain,
        String domainVersion,
        Map<String, Object> data,
        String replyToMid,
        List<EditChannelMessageCommand.MentionTargetCommand> mentions,
        String clientMessageId
) {
}
