package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道审计动作映射协作对象。
 * 职责：维护审计日志外部 action 名称与内部 actionType 的稳定转换关系。
 * 边界：不访问仓储、不决定审计记录可见性、不改变审计事件写入语义。
 */
class ChannelAuditActionMapper {

    String normalizeFilterAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return switch (action.trim()) {
            case "channel.create" -> "CHANNEL_CREATED";
            case "channel.delete" -> "CHANNEL_DELETED";
            case "channel.update" -> "CHANNEL_UPDATED";
            case "channel.member.kick" -> "MEMBER_KICKED";
            case "channel.admin.grant" -> "MEMBER_PROMOTED_TO_ADMIN";
            case "channel.admin.revoke" -> "ADMIN_DEMOTED_TO_MEMBER";
            case "channel.ban.create" -> "MEMBER_BANNED";
            case "channel.ban.delete" -> "MEMBER_UNBANNED";
            case "message.delete" -> "MESSAGE_DELETED";
            case "message.edit" -> "MESSAGE_EDITED";
            case "message.pin" -> "MESSAGE_PINNED";
            case "message.unpin" -> "MESSAGE_UNPINNED";
            default -> throw ProblemException.validationFailed("action is invalid");
        };
    }

    String toClientAction(String actionType) {
        return switch (actionType) {
            case "CHANNEL_CREATED" -> "channel.create";
            case "CHANNEL_DELETED" -> "channel.delete";
            case "CHANNEL_UPDATED" -> "channel.update";
            case "MEMBER_KICKED" -> "channel.member.kick";
            case "MEMBER_PROMOTED_TO_ADMIN" -> "channel.admin.grant";
            case "ADMIN_DEMOTED_TO_MEMBER" -> "channel.admin.revoke";
            case "MEMBER_BANNED" -> "channel.ban.create";
            case "MEMBER_UNBANNED" -> "channel.ban.delete";
            case "MESSAGE_DELETED" -> "message.delete";
            case "MESSAGE_EDITED" -> "message.edit";
            case "MESSAGE_PINNED" -> "message.pin";
            case "MESSAGE_UNPINNED" -> "message.unpin";
            default -> actionType.toLowerCase();
        };
    }
}
