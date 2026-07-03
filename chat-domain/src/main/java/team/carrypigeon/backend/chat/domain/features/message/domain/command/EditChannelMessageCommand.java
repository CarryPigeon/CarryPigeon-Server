package team.carrypigeon.backend.chat.domain.features.message.domain.command;

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

    /**
     * 编辑消息命令中的 mention 目标。
     * 职责：以领域命令形式表达需要写入消息的提及对象。
     */
    public record MentionTargetCommand(String type, long uid) {
    }
}
