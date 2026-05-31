package team.carrypigeon.backend.chat.domain.features.message.application.command;

import java.util.List;

/**
 * 编辑频道消息命令。
 */
public record EditChannelMessageCommand(
        long accountId,
        long messageId,
        String domain,
        String domainVersion,
        String text,
        List<MentionTargetCommand> mentions,
        Long expectedEditVersion
) {

    public record MentionTargetCommand(String type, long uid) {
    }
}
