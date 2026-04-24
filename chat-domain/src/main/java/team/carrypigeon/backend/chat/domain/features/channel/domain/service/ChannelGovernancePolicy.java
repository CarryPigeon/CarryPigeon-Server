package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道治理规则组件。
 * 职责：集中承载当前切片内 private channel 的成员治理授权与状态判断规则。
 * 边界：仅覆盖已确认的 OWNER / ADMIN / MEMBER 固定角色与邀请相关规则，不扩展为通用权限引擎。
 */
@Component
public class ChannelGovernancePolicy {

    private static final String PRIVATE_CHANNEL_REQUIRED_MESSAGE = "channel invite flow is only supported for private channels";
    private static final String INVITE_ROLE_REQUIRED_MESSAGE = "channel invite requires owner or admin role";
    private static final String CHANNEL_INVITE_NOT_PENDING_MESSAGE = "channel invite is not pending";
    private static final String CHANNEL_BAN_ACTIVE_MESSAGE = "channel ban is still active";
    private static final String OWNER_ROLE_REQUIRED_MESSAGE = "channel action requires owner role";
    private static final String OWNER_OR_ADMIN_ROLE_REQUIRED_MESSAGE = "channel action requires owner or admin role";
    private static final String TARGET_MEMBER_ROLE_REQUIRED_MESSAGE = "target member must have MEMBER role";
    private static final String TARGET_ADMIN_ROLE_REQUIRED_MESSAGE = "target member must have ADMIN role";
    private static final String TARGET_OWNER_FORBIDDEN_MESSAGE = "target member with OWNER role cannot be moderated";
    private static final String CHANNEL_MEMBER_MUTED_MESSAGE = "channel member is muted";
    private static final String CHANNEL_MESSAGE_RECALL_FORBIDDEN_MESSAGE = "channel message recall is not allowed";

    /**
     * 断言当前频道支持私有频道邀请流程。
     *
     * @param channel 待校验频道
     */
    public void requirePrivateChannel(Channel channel) {
        if (!"private".equals(channel.type())) {
            throw ProblemException.forbidden("private_channel_required", PRIVATE_CHANNEL_REQUIRED_MESSAGE);
        }
    }

    /**
     * 断言成员具备邀请权限。
     *
     * @param channel 频道
     * @param operator 执行邀请的成员
     */
    public void requireCanInvite(Channel channel, ChannelMember operator) {
        requirePrivateChannel(channel);
        requireOwnerOrAdmin(operator, "channel_invite_forbidden", INVITE_ROLE_REQUIRED_MESSAGE);
    }

    /**
     * 断言成员具备查看成员列表权限。
     *
     * @param operator 当前活跃成员
     */
    public void requireCanListMembers(ChannelMember operator) {
        if (operator == null) {
            throw ProblemException.forbidden("channel_member_required", "channel membership is required");
        }
    }

    /**
     * 断言邀请可由当前账户接受。
     *
     * @param channel 频道
     * @param invite 邀请记录
     * @param accountId 当前账户 ID
     */
    public void requireCanAcceptInvite(Channel channel, ChannelInvite invite, long accountId) {
        requirePrivateChannel(channel);
        if (invite.inviteeAccountId() != accountId) {
            throw ProblemException.forbidden("channel_invite_forbidden", "channel invite is not granted to current account");
        }
        if (invite.status() != ChannelInviteStatus.PENDING) {
            throw ProblemException.validationFailed(CHANNEL_INVITE_NOT_PENDING_MESSAGE);
        }
    }

    /**
     * 判断频道封禁是否仍然生效。
     *
     * @param channelBan 封禁记录
     * @param now 当前时间
     * @return 当前时刻仍然生效时返回 true
     */
    public boolean isBanActive(ChannelBan channelBan, Instant now) {
        return channelBan != null
                && channelBan.revokedAt() == null
                && (channelBan.expiresAt() == null || channelBan.expiresAt().isAfter(now));
    }

    /**
     * 断言频道封禁当前未生效。
     *
     * @param channelBan 封禁记录
     * @param now 当前时间
     */
    public void requireBanInactive(ChannelBan channelBan, Instant now) {
        if (isBanActive(channelBan, now)) {
            throw ProblemException.forbidden("channel_ban_active", CHANNEL_BAN_ACTIVE_MESSAGE);
        }
    }

    /**
     * 断言当前成员可被提升为 ADMIN。
     *
     * @param channel 频道
     * @param operator 操作人
     * @param target 目标成员
     */
    public void requireCanPromoteToAdmin(Channel channel, ChannelMember operator, ChannelMember target) {
        requirePrivateChannel(channel);
        requireOwner(operator, "channel_role_forbidden", OWNER_ROLE_REQUIRED_MESSAGE);
        requireTargetRole(target, ChannelMemberRole.MEMBER, TARGET_MEMBER_ROLE_REQUIRED_MESSAGE);
    }

    /**
     * 断言当前成员可被降级为 MEMBER。
     *
     * @param channel 频道
     * @param operator 操作人
     * @param target 目标成员
     */
    public void requireCanDemoteAdmin(Channel channel, ChannelMember operator, ChannelMember target) {
        requirePrivateChannel(channel);
        requireOwner(operator, "channel_role_forbidden", OWNER_ROLE_REQUIRED_MESSAGE);
        requireTargetRole(target, ChannelMemberRole.ADMIN, TARGET_ADMIN_ROLE_REQUIRED_MESSAGE);
    }

    /**
     * 断言当前成员可转移频道所有权。
     *
     * @param channel 频道
     * @param operator 操作人
     * @param target 新 OWNER 目标成员
     */
    public void requireCanTransferOwnership(Channel channel, ChannelMember operator, ChannelMember target) {
        requirePrivateChannel(channel);
        requireOwner(operator, "channel_ownership_forbidden", OWNER_ROLE_REQUIRED_MESSAGE);
        if (target.role() == ChannelMemberRole.OWNER) {
            throw ProblemException.validationFailed("target member is already owner");
        }
    }

    /**
     * 断言当前成员可对目标成员执行 MEMBER 级治理动作。
     *
     * @param channel 频道
     * @param operator 操作人
     * @param target 目标成员
     * @param errorCode 权限错误码
     */
    public void requireCanModerateMember(Channel channel, ChannelMember operator, ChannelMember target, String errorCode) {
        requirePrivateChannel(channel);
        requireOwnerOrAdmin(operator, errorCode, OWNER_OR_ADMIN_ROLE_REQUIRED_MESSAGE);
        if (target.role() == ChannelMemberRole.OWNER) {
            throw ProblemException.forbidden(errorCode, TARGET_OWNER_FORBIDDEN_MESSAGE);
        }
        requireTargetRole(target, ChannelMemberRole.MEMBER, TARGET_MEMBER_ROLE_REQUIRED_MESSAGE);
    }

    /**
     * 断言当前成员可解除封禁。
     *
     * @param channel 频道
     * @param operator 操作人
     */
    public void requireCanUnban(Channel channel, ChannelMember operator) {
        requirePrivateChannel(channel);
        requireOwnerOrAdmin(operator, "channel_ban_forbidden", OWNER_OR_ADMIN_ROLE_REQUIRED_MESSAGE);
    }

    /**
     * 断言当前成员可发送消息。
     *
     * @param channel 频道
     * @param member 当前成员
     * @param now 当前时间
     */
    public void requireCanSendMessage(Channel channel, ChannelMember member, Instant now) {
        if (!"private".equals(channel.type())) {
            return;
        }
        if (member.mutedUntil() != null && member.mutedUntil().isAfter(now)) {
            throw ProblemException.forbidden("channel_member_muted", CHANNEL_MEMBER_MUTED_MESSAGE);
        }
    }

    /**
     * 断言当前成员可撤回目标消息。
     *
     * @param channel 频道
     * @param operator 当前活跃成员
     * @param message 待撤回消息
     * @param senderMember 消息发送者当前活跃成员投影，不存在时表示 former / non-active
     */
    public void requireCanRecallMessage(
            Channel channel,
            ChannelMember operator,
            team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage message,
            ChannelMember senderMember
    ) {
        if (operator.accountId() == message.senderId()) {
            return;
        }
        if (!"private".equals(channel.type())) {
            throw ProblemException.forbidden("channel_message_recall_forbidden", CHANNEL_MESSAGE_RECALL_FORBIDDEN_MESSAGE);
        }
        if (operator.role() == ChannelMemberRole.OWNER) {
            return;
        }
        if (operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.forbidden("channel_message_recall_forbidden", CHANNEL_MESSAGE_RECALL_FORBIDDEN_MESSAGE);
        }
        if (senderMember == null) {
            return;
        }
        if (senderMember.role() == ChannelMemberRole.OWNER) {
            throw ProblemException.forbidden("channel_message_recall_forbidden", TARGET_OWNER_FORBIDDEN_MESSAGE);
        }
        if (senderMember.role() != ChannelMemberRole.MEMBER) {
            throw ProblemException.forbidden("channel_message_recall_forbidden", CHANNEL_MESSAGE_RECALL_FORBIDDEN_MESSAGE);
        }
    }

    private void requireOwner(ChannelMember operator, String errorCode, String message) {
        if (operator.role() != ChannelMemberRole.OWNER) {
            throw ProblemException.forbidden(errorCode, message);
        }
    }

    private void requireOwnerOrAdmin(ChannelMember operator, String errorCode, String message) {
        if (operator.role() != ChannelMemberRole.OWNER && operator.role() != ChannelMemberRole.ADMIN) {
            throw ProblemException.forbidden(errorCode, message);
        }
    }

    private void requireTargetRole(ChannelMember target, ChannelMemberRole expectedRole, String message) {
        if (target.role() != expectedRole) {
            throw ProblemException.validationFailed(message);
        }
    }
}
