package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道命令校验协作对象。
 * 职责：集中处理频道写侧命令基础校验、文本规范化和审计 metadata 构造。
 * 边界：不访问仓储、不判断权限、不修改频道状态。
 */
class ChannelCommandValidator {

    void validateCreatePrivateChannelCommand(CreatePrivateChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        requireChannelName(command.name());
    }

    void validateCreateChannelCommand(CreateChannelCommand command) {
        requirePositive(command.accountId(), "accountId");
        requireChannelName(command.name());
        requireBriefLength(command.brief());
    }

    void validateUpdateChannelProfileCommand(UpdateChannelProfileCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        requireChannelName(command.name());
        requireBriefLength(command.brief());
    }

    void validateDeleteChannelCommand(DeleteChannelCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
    }

    void validateInviteChannelMemberCommand(InviteChannelMemberCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.inviteeAccountId(), "inviteeAccountId");
        if (command.operatorAccountId() == command.inviteeAccountId()) {
            throw ProblemException.validationFailed("operatorAccountId must not equal inviteeAccountId");
        }
    }

    void validateAcceptChannelInviteCommand(AcceptChannelInviteCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
    }

    void validateUpdateChannelReadStateCommand(UpdateChannelReadStateCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.lastReadMessageId(), "lastReadMessageId");
        if (command.lastReadTime() <= 0) {
            throw ProblemException.validationFailed("lastReadTime must be greater than 0");
        }
    }

    void validateTargetedCommand(long operatorAccountId, long channelId, long targetAccountId, String targetFieldName) {
        requirePositive(operatorAccountId, "operatorAccountId");
        requirePositive(channelId, "channelId");
        requirePositive(targetAccountId, targetFieldName);
        if (operatorAccountId == targetAccountId) {
            throw ProblemException.validationFailed("operatorAccountId must not equal " + targetFieldName);
        }
    }

    void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return reason.trim();
    }

    String normalizeNullableText(String value) {
        return value == null ? "" : value.trim();
    }

    String buildBanAuditMetadata(ChannelBan ban) {
        StringBuilder metadata = new StringBuilder("{");
        metadata.append("\"expiresAt\":");
        if (ban.expiresAt() == null) {
            metadata.append("null");
        } else {
            metadata.append("\"").append(ban.expiresAt()).append("\"");
        }
        metadata.append('}');
        return metadata.toString();
    }

    private void requireChannelName(String name) {
        if (name == null || name.isBlank()) {
            throw ProblemException.validationFailed("name must not be blank");
        }
        if (name.trim().length() > 128) {
            throw ProblemException.validationFailed("name length must be less than or equal to 128");
        }
    }

    private void requireBriefLength(String brief) {
        if (brief != null && brief.trim().length() > 256) {
            throw ProblemException.validationFailed("brief length must be less than or equal to 256");
        }
    }
}
