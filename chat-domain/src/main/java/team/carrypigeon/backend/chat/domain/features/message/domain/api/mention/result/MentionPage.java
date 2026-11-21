package team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.result;

import java.util.List;

/** 按提及 ID 倒序的一页收件箱记录。仅有下一页时提供排他上界。 */
public record MentionPage(List<MentionResult> items, Long nextMentionId) {
}
