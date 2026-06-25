package team.carrypigeon.backend.chat.domain.features.channel.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.AuditLogResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.dto.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.DiscoverChannelsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListAuditLogsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.application.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;

/**
 * 频道查询应用服务。
 * 职责：集中承接频道目录、成员列表、封禁列表、审计日志与未读统计等读侧能力。
 * 边界：这里只处理查询规则与结果投影，不承担频道写入、事务副作用或广播。
 */
@Service
public class ChannelQueryApplicationService {

    private static final String PUBLIC_CHANNEL_TYPE = "public";
    private static final String PRIVATE_CHANNEL_TYPE = "private";
    private static final String SYSTEM_CHANNEL_TYPE = "system";
    private static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String SYSTEM_CHANNEL_MEMBERS_HIDDEN_MESSAGE = "system channel member list is not available";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelBanRepository channelBanRepository;
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final ChannelReadStateRepository channelReadStateRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;

    public ChannelQueryApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            UserProfileRepository userProfileRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelReadStateRepository = channelReadStateRepository;
        this.userProfileRepository = userProfileRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
    }

    public List<ChannelMemberResult> listChannelMembers(ListChannelMembersQuery query) {
        validateListChannelMembersQuery(query);
        Channel channel = requireChannel(query.channelId());
        if (SYSTEM_CHANNEL_TYPE.equals(channel.type())) {
            throw ProblemException.forbidden("system_channel_members_hidden", SYSTEM_CHANNEL_MEMBERS_HIDDEN_MESSAGE);
        }
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        channelGovernancePolicy.requireCanListMembers(operator);
        return channelMemberRepository.findByChannelId(channel.id()).stream()
                .map(this::toMemberResult)
                .toList();
    }

    public List<ChannelBanListItemResult> listChannelBans(ListChannelBansQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
        Channel channel = requireChannel(query.channelId());
        ChannelMember operator = requireMember(channel.id(), query.accountId());
        channelGovernancePolicy.requireCanUnban(channel, operator);
        return channelBanRepository.findByChannelId(channel.id()).stream()
                .map(this::toBanListItemResult)
                .toList();
    }

    public List<AuditLogResult> listAuditLogs(ListAuditLogsQuery query) {
        requirePositive(query.accountId(), "accountId");
        if (query.cursorAuditId() != null && query.cursorAuditId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
        if (query.channelId() != null) {
            requirePositive(query.channelId(), "channelId");
        }
        if (query.actorAccountId() != null) {
            requirePositive(query.actorAccountId(), "actorAccountId");
        }
        if (query.fromTime() != null && query.fromTime() < 0) {
            throw ProblemException.validationFailed("from_time must be greater than or equal to 0");
        }
        if (query.toTime() != null && query.toTime() < 0) {
            throw ProblemException.validationFailed("to_time must be greater than or equal to 0");
        }
        return channelAuditLogRepository.list(
                        query.cursorAuditId(),
                        query.limit() + 1,
                        query.channelId(),
                        query.actorAccountId(),
                        normalizeAuditAction(query.action()),
                        query.fromTime() == null ? null : Instant.ofEpochMilli(query.fromTime()),
                        query.toTime() == null ? null : Instant.ofEpochMilli(query.toTime())
                ).stream()
                .map(log -> new AuditLogResult(
                        Ids.toString(log.auditId()),
                        Ids.toString(log.channelId()),
                        log.actorAccountId() == null ? null : Ids.toString(log.actorAccountId()),
                        toAuditAction(log.actionType()),
                        log.metadata(),
                        log.createdAt().toEpochMilli()
                ))
                .toList();
    }

    public List<ChannelResult> listChannels(long accountId) {
        requirePositive(accountId, "accountId");
        ArrayList<ChannelResult> channels = new ArrayList<>();
        channelRepository.findDefaultChannel().ifPresent(channel -> channels.add(toResult(channel)));
        channelRepository.findSystemChannel()
                .filter(channel -> channelMemberRepository.exists(channel.id(), accountId))
                .ifPresent(channel -> channels.add(toResult(channel)));
        for (Long channelId : channelMemberRepository.findChannelIdsByAccountId(accountId)) {
            channelRepository.findById(channelId)
                    .filter(channel -> channels.stream().noneMatch(existing -> existing.channelId() == channel.id()))
                    .ifPresent(channel -> channels.add(toResult(channel)));
        }
        return channels;
    }

    public ChannelResult getChannelById(long accountId, long channelId) {
        requirePositive(accountId, "accountId");
        requirePositive(channelId, "channelId");
        Channel channel = requireChannel(channelId);
        if (SYSTEM_CHANNEL_TYPE.equals(channel.type()) && !channelMemberRepository.exists(channel.id(), accountId)) {
            throw ProblemException.forbidden("system_channel_membership_required", MEMBERSHIP_REQUIRED_MESSAGE);
        }
        return toResult(channel);
    }

    public List<DiscoverChannelResult> discoverChannels(DiscoverChannelsQuery query) {
        requirePositive(query.accountId(), "accountId");
        if (query.cursorChannelId() != null && query.cursorChannelId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 50) {
            throw ProblemException.validationFailed("limit must be between 1 and 50");
        }
        String normalizedType = normalizeChannelTypeFilter(query.type());
        return channelRepository.discoverChannels(
                        query.keyword() == null || query.keyword().isBlank() ? null : query.keyword().trim(),
                        query.cursorChannelId(),
                        normalizedType,
                        query.limit() + 1
                ).stream()
                .map(channel -> new DiscoverChannelResult(
                        Ids.toString(channel.id()),
                        channel.name(),
                        channel.brief(),
                        channel.avatar(),
                        channel.memberCount(),
                        channel.requiresApplication()
                ))
                .toList();
    }

    public List<ChannelUnreadResult> listUnreads(long accountId) {
        requirePositive(accountId, "accountId");
        return channelReadStateRepository.listUnreadsByAccountId(accountId).stream()
                .map(item -> new ChannelUnreadResult(
                        Ids.toString(item.channelId()),
                        item.unreadCount(),
                        item.lastReadTime() == null ? 0L : item.lastReadTime().toEpochMilli()
                ))
                .toList();
    }

    private ChannelResult toResult(Channel channel) {
        return new ChannelResult(
                channel.id(),
                channel.conversationId(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                findOwnerUid(channel.id()),
                channel.type(),
                channel.defaultChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }

    private ChannelMemberResult toMemberResult(ChannelMember member) {
        team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile userProfile =
                userProfileRepository.findByAccountId(member.accountId()).orElse(null);
        return new ChannelMemberResult(
                member.accountId(),
                userProfile == null ? "" : userProfile.nickname(),
                userProfile == null ? "" : userProfile.avatarUrl(),
                member.role().name(),
                member.joinedAt(),
                member.mutedUntil()
        );
    }

    private ChannelBanListItemResult toBanListItemResult(ChannelBan ban) {
        return new ChannelBanListItemResult(
                ban.channelId(),
                ban.bannedAccountId(),
                ban.expiresAt(),
                ban.reason(),
                ban.createdAt()
        );
    }

    private void validateListChannelMembersQuery(ListChannelMembersQuery query) {
        requirePositive(query.accountId(), "accountId");
        requirePositive(query.channelId(), "channelId");
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private String findOwnerUid(long channelId) {
        return channelMemberRepository.findByChannelId(channelId).stream()
                .filter(member -> member.role() == ChannelMemberRole.OWNER)
                .map(member -> Ids.toString(member.accountId()))
                .findFirst()
                .orElse("");
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    private String normalizeAuditAction(String action) {
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

    private String normalizeChannelTypeFilter(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalizedType = type.trim().toLowerCase();
        if (!List.of(PUBLIC_CHANNEL_TYPE, PRIVATE_CHANNEL_TYPE, SYSTEM_CHANNEL_TYPE).contains(normalizedType)) {
            throw ProblemException.validationFailed("type is invalid");
        }
        return normalizedType;
    }

    private String toAuditAction(String actionType) {
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
