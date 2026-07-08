package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelQueryApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.AuditLogResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanListItemResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.DiscoverChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.DiscoverChannelsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListAuditLogsQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelBansQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
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
 * 频道查询领域服务。
 * 职责：集中承接频道目录、成员列表、封禁列表、审计日志与未读统计等读侧能力。
 * 边界：这里只处理查询规则与结果投影，不承担频道写入、事务副作用或广播。
 */
@Service
public class ChannelQueryDomainApi implements ChannelQueryApi {

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
    private final ChannelGovernancePolicy channelGovernancePolicy;
    private final ChannelProjectionMapper channelProjectionMapper;
    private final ChannelAuditActionMapper channelAuditActionMapper;

    public ChannelQueryDomainApi(
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
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.channelProjectionMapper = new ChannelProjectionMapper(channelMemberRepository, userProfileRepository);
        this.channelAuditActionMapper = new ChannelAuditActionMapper();
    }

    /**
     * 查询频道成员列表。
     * 约束：调用方必须是频道成员；system 频道隐藏成员列表。
     *
     * @param query 成员列表查询
     * @return 频道成员投影列表
     */
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

    /**
     * 查询频道封禁列表。
     * 约束：调用方必须具备解除封禁权限。
     *
     * @param query 封禁列表查询
     * @return 封禁列表项投影
     */
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

    /**
     * 查询频道审计日志。
     * 语义：支持游标、动作、时间和操作人过滤，并额外读取一条用于调用方判断是否还有下一页。
     *
     * @param query 审计日志查询
     * @return 审计日志投影列表
     */
    public List<AuditLogResult> listAuditLogs(ListAuditLogsQuery query) {
        requirePositive(query.accountId(), "accountId");
        if (query.cursorAuditId() != null && query.cursorAuditId() <= 0) {
            throw ProblemException.validationFailed("cursor_invalid", "cursor is invalid");
        }
        if (query.limit() <= 0 || query.limit() > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
        if (query.channelId() == null) {
            throw ProblemException.validationFailed("channelId must not be null");
        }
        requirePositive(query.channelId(), "channelId");
        requireMember(query.channelId(), query.accountId());
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
                        channelAuditActionMapper.normalizeFilterAction(query.action()),
                        query.fromTime() == null ? null : Instant.ofEpochMilli(query.fromTime()),
                        query.toTime() == null ? null : Instant.ofEpochMilli(query.toTime())
                ).stream()
                .map(log -> new AuditLogResult(
                        Ids.toString(log.auditId()),
                        Ids.toString(log.channelId()),
                        log.actorAccountId() == null ? null : Ids.toString(log.actorAccountId()),
                        channelAuditActionMapper.toClientAction(log.actionType()),
                        log.metadata(),
                        log.createdAt().toEpochMilli()
                ))
                .toList();
    }

    /**
     * 查询账号可见频道列表。
     * 语义：默认频道总是返回，system 频道仅对成员返回，普通成员频道按成员关系补齐。
     *
     * @param accountId 当前账号 ID
     * @return 可见频道投影列表
     */
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

    /**
     * 按 ID 查询账号可见频道。
     * 约束：system 与 private 频道只允许成员访问。
     *
     * @param accountId 当前账号 ID
     * @param channelId 频道 ID
     * @return 频道投影
     */
    public ChannelResult getChannelById(long accountId, long channelId) {
        requirePositive(accountId, "accountId");
        requirePositive(channelId, "channelId");
        Channel channel = requireChannel(channelId);
        if ((SYSTEM_CHANNEL_TYPE.equals(channel.type()) || PRIVATE_CHANNEL_TYPE.equals(channel.type()))
                && !channelMemberRepository.exists(channel.id(), accountId)) {
            throw ProblemException.forbidden("channel_membership_required", MEMBERSHIP_REQUIRED_MESSAGE);
        }
        return toResult(channel);
    }

    /**
     * 发现可加入或可浏览的频道。
     * 语义：按关键词、频道类型和游标查询公开发现列表，返回申请准入标识。
     *
     * @param query 频道发现查询
     * @return 频道发现投影列表
     */
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

    /**
     * 查询账号各频道未读统计。
     *
     * @param accountId 当前账号 ID
     * @return 频道未读投影列表
     */
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
        return channelProjectionMapper.toResult(channel);
    }

    private ChannelMemberResult toMemberResult(ChannelMember member) {
        return channelProjectionMapper.toMemberResult(member);
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

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
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
}
