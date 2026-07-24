package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.List;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AcceptChannelInviteCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.InviteChannelMemberCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelApplicationResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelBanResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelInviteResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMemberResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.MessageReferenceApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageReferenceResult;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * channel 命令侧领域服务共享支撑。
 * 职责：收敛频道能力服务共用的仓储访问、校验、结果投影、审计和实时广播辅助逻辑。
 * 边界：仅供 channel feature 内部领域服务复用，不作为对外稳定入口。
 */
abstract class AbstractChannelDomainSupport {

    protected static final String CHANNEL_NOT_FOUND_MESSAGE = "default channel does not exist";
    protected static final String SYSTEM_CHANNEL_NOT_FOUND_MESSAGE = "system channel does not exist";
    protected static final String GENERAL_CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    protected static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    protected static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    protected static final String CHANNEL_INVITE_NOT_FOUND_MESSAGE = "channel invite does not exist";
    protected static final String CHANNEL_MEMBER_NOT_FOUND_MESSAGE = "channel member does not exist";
    protected static final String CHANNEL_BAN_NOT_FOUND_MESSAGE = "channel ban does not exist";

    protected final ChannelRepository channelRepository;
    protected final ChannelMemberRepository channelMemberRepository;
    protected final ChannelInviteRepository channelInviteRepository;
    protected final ChannelBanRepository channelBanRepository;
    protected final ChannelAuditLogRepository channelAuditLogRepository;
    protected final ChannelReadStateRepository channelReadStateRepository;
    protected final MessageReferenceApi messageReferenceApi;
    protected final UserProfileApi userProfileApi;
    protected final ChannelGovernancePolicy channelGovernancePolicy;
    protected final ChannelAfterCommitPublisher channelAfterCommitPublisher;
    protected final ChannelProjectionMapper channelProjectionMapper;
    protected final ChannelCommandValidator channelCommandValidator;
    protected final IdGenerator idGenerator;
    protected final TimeProvider timeProvider;
    protected final TransactionRunner transactionRunner;

    protected AbstractChannelDomainSupport(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            MessageReferenceApi messageReferenceApi,
            UserProfileApi userProfileApi,
            ChannelGovernancePolicy channelGovernancePolicy,
            RealtimeEventApi realtimeEventApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelInviteRepository = channelInviteRepository;
        this.channelBanRepository = channelBanRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelReadStateRepository = channelReadStateRepository;
        this.messageReferenceApi = messageReferenceApi;
        this.userProfileApi = userProfileApi;
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.channelAfterCommitPublisher = new ChannelAfterCommitPublisher(realtimeEventApi);
        this.channelProjectionMapper = new ChannelProjectionMapper(channelMemberRepository, userProfileApi);
        this.channelCommandValidator = new ChannelCommandValidator();
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 获取频道当前成员账号快照。
     * 语义：用于事务提交后事件的接收人列表，避免后续成员变化影响本次事件范围。
     *
     * @param channelId 频道 ID
     * @return 当前频道成员账号 ID 列表快照
     */
    protected List<Long> snapshotChannelRecipientAccountIds(long channelId) {
        return List.copyOf(channelMemberRepository.findAccountIdsByChannelId(channelId));
    }

    /**
     * 将频道聚合转换为对外投影。
     * 边界：统一复用频道投影映射规则，避免各写侧服务自行拼装 owner 等派生字段。
     *
     * @param channel 频道聚合
     * @return 频道结果投影
     */
    protected ChannelResult toResult(Channel channel) {
        return channelProjectionMapper.toResult(channel);
    }

    /**
     * 将邀请记录转换为邀请流程投影。
     *
     * @param invite 频道邀请记录
     * @return 邀请结果投影
     */
    protected ChannelInviteResult toInviteResult(ChannelInvite invite) {
        return new ChannelInviteResult(
                invite.channelId(),
                invite.inviteeAccountId(),
                invite.inviterAccountId(),
                invite.status().name(),
                invite.createdAt(),
                invite.respondedAt()
                );
    }

    /**
     * 将邀请记录转换为入群申请投影。
     * 语义：当前模型复用 invite 存储申请状态，reason 来自持久化邀请记录。
     *
     * @param invite 复用邀请模型承载的申请记录
     * @param reason 申请理由，缺失时回退使用邀请记录内的理由
     * @return 入群申请结果投影
     */
    protected ChannelApplicationResult toApplicationResult(ChannelInvite invite, String reason) {
        return new ChannelApplicationResult(
                invite.applicationId(),
                invite.channelId(),
                invite.inviteeAccountId(),
                normalizeApplicationReason(reason == null ? invite.reason() : reason),
                invite.createdAt(),
                invite.status().name()
                );
    }

    private String normalizeApplicationReason(String reason) {
        return reason == null ? "" : reason;
    }

    /**
     * 判断一条邀请记录是否为入群申请。
     * 约束：申请使用特殊 inviter 标记区分，不改变底层邀请模型结构。
     *
     * @param invite 待判断的邀请记录
     * @return true 表示该记录承载入群申请
     */
    protected boolean isChannelApplication(ChannelInvite invite) {
        return invite.inviterAccountId() == channelApplicationMarkerAccountId(invite.inviteeAccountId());
    }

    /**
     * 生成入群申请记录的 inviter 标记账号。
     * 语义：在复用 invite 模型期间，申请方账号同时作为稳定标记值。
     *
     * @param inviteeAccountId 申请加入频道的账号 ID
     * @return 用于标记申请记录的账号 ID
     */
    protected long channelApplicationMarkerAccountId(long inviteeAccountId) {
        return inviteeAccountId;
    }

    /**
     * 校验频道是否允许被物理删除。
     * 失败语义：存在邀请、封禁、消息或审计等依赖数据时抛出冲突问题。
     *
     * @param channelId 待删除频道 ID
     */
    protected void requireChannelDeleteSafe(long channelId) {
        if (!channelInviteRepository.findByChannelId(channelId).isEmpty()
                || !channelBanRepository.findByChannelId(channelId).isEmpty()
                || messageReferenceApi.hasChannelMessages(channelId)
                || !channelAuditLogRepository.list(null, 1, channelId, null, null, null, null).isEmpty()) {
            throw ProblemException.conflict(
                    "channel_delete_blocked",
                    "channel contains dependent data and cannot be deleted"
            );
        }
    }

    /**
     * 将封禁记录转换为封禁结果投影。
     *
     * @param ban 频道封禁记录
     * @return 封禁结果投影
     */
    protected ChannelBanResult toBanResult(ChannelBan ban) {
        return new ChannelBanResult(
                ban.channelId(),
                ban.bannedAccountId(),
                ban.operatorAccountId(),
                ban.reason(),
                ban.expiresAt(),
                ban.createdAt(),
                ban.revokedAt()
                );
    }

    /**
     * 将成员记录转换为成员结果投影。
     * 边界：统一补齐成员展示字段，调用方不直接访问用户资料仓储。
     *
     * @param member 频道成员记录
     * @return 成员结果投影
     */
    protected ChannelMemberResult toMemberResult(ChannelMember member) {
        return channelProjectionMapper.toMemberResult(member);
    }

    /**
     * 校验私有频道创建命令的业务前置条件。
     *
     * @param command 私有频道创建命令
     */
    protected void validateCreatePrivateChannelCommand(CreatePrivateChannelCommand command) {
        channelCommandValidator.validateCreatePrivateChannelCommand(command);
    }

    /**
     * 校验频道创建命令的业务前置条件。
     *
     * @param command 频道创建命令
     */
    protected void validateCreateChannelCommand(CreateChannelCommand command) {
        channelCommandValidator.validateCreateChannelCommand(command);
    }

    /**
     * 校验频道资料更新命令的业务前置条件。
     *
     * @param command 频道资料更新命令
     */
    protected void validateUpdateChannelProfileCommand(UpdateChannelProfileCommand command) {
        channelCommandValidator.validateUpdateChannelProfileCommand(command);
    }

    /**
     * 校验频道删除命令的业务前置条件。
     *
     * @param command 频道删除命令
     */
    protected void validateDeleteChannelCommand(DeleteChannelCommand command) {
        channelCommandValidator.validateDeleteChannelCommand(command);
    }

    /**
     * 校验成员邀请命令的业务前置条件。
     *
     * @param command 成员邀请命令
     */
    protected void validateInviteChannelMemberCommand(InviteChannelMemberCommand command) {
        channelCommandValidator.validateInviteChannelMemberCommand(command);
    }

    /**
     * 校验接受邀请命令的业务前置条件。
     *
     * @param command 接受邀请命令
     */
    protected void validateAcceptChannelInviteCommand(AcceptChannelInviteCommand command) {
        channelCommandValidator.validateAcceptChannelInviteCommand(command);
    }

    /**
     * 校验频道已读状态更新命令的业务前置条件。
     *
     * @param command 已读状态更新命令
     */
    protected void validateUpdateChannelReadStateCommand(UpdateChannelReadStateCommand command) {
        channelCommandValidator.validateUpdateChannelReadStateCommand(command);
    }

    /**
     * 校验面向目标成员的治理命令公共前置条件。
     *
     * @param operatorAccountId 操作人账号 ID
     * @param channelId 频道 ID
     * @param targetAccountId 目标成员账号 ID
     * @param targetFieldName 目标账号字段名，用于生成校验失败消息
     */
    protected void validateTargetedCommand(long operatorAccountId, long channelId, long targetAccountId, String targetFieldName) {
        channelCommandValidator.validateTargetedCommand(operatorAccountId, channelId, targetAccountId, targetFieldName);
    }

    /**
     * 读取必须存在的频道。
     * 失败语义：频道不存在时抛出 not found 问题。
     *
     * @param channelId 频道 ID
     * @return 频道聚合
     */
    protected Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(GENERAL_CHANNEL_NOT_FOUND_MESSAGE));
    }

    /**
     * 读取必须存在的消息快照。
     * 边界：通过 message feature 领域 API 访问消息，不直接依赖消息仓储。
     *
     * @param messageId 消息 ID
     * @return 消息快照
     */
    protected MessageReferenceResult requireMessage(long messageId) {
        return messageReferenceApi.requireMessage(messageId);
    }

    /**
     * 查询频道所有者展示 UID。
     * 语义：作为频道投影补充字段，缺失处理由统一投影映射器负责。
     *
     * @param channelId 频道 ID
     * @return 所有者 UID
     */
    protected String findOwnerUid(long channelId) {
        return channelProjectionMapper.findOwnerUid(channelId);
    }

    /**
     * 读取调用方在频道中的成员身份。
     * 失败语义：非频道成员按权限不足处理。
     *
     * @param channelId 频道 ID
     * @param accountId 账号 ID
     * @return 频道成员记录
     */
    protected ChannelMember requireMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    /**
     * 读取治理目标成员。
     * 失败语义：目标账号不是频道成员时按资源不存在处理。
     *
     * @param channelId 频道 ID
     * @param accountId 目标账号 ID
     * @return 目标成员记录
     */
    protected ChannelMember requireTargetMember(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_MEMBER_NOT_FOUND_MESSAGE));
    }

    /**
     * 校验被邀请账号是否存在用户资料。
     *
     * @param inviteeAccountId 被邀请账号 ID
     */
    protected void requireInviteeExists(long inviteeAccountId) {
        if (userProfileApi.getPublicUserProfiles(List.of(inviteeAccountId)).isEmpty()) {
            throw ProblemException.notFound("invitee account does not exist");
        }
    }

    /**
     * 追加频道审计日志。
     * 副作用：写入审计仓储，metadata 为空时统一保存为空 JSON 对象。
     *
     * @param channelId 频道 ID
     * @param actorAccountId 操作人账号 ID
     * @param actionType 领域审计动作类型
     * @param targetAccountId 目标账号 ID，可为空
     * @param metadata 审计扩展信息，可为空
     */
    protected void appendAuditLog(long channelId, long actorAccountId, String actionType, Long targetAccountId, String metadata) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                nextId(),
                channelId,
                actorAccountId,
                actionType,
                targetAccountId,
                metadata == null ? "{}" : metadata,
                now()
                ));
    }

    /**
     * 创建新的频道成员记录。
     *
     * @param channelId 频道 ID
     * @param accountId 成员账号 ID
     * @param role 初始成员角色
     * @return 新成员记录
     */
    protected ChannelMember newMember(long channelId, long accountId, ChannelMemberRole role) {
        return new ChannelMember(channelId, accountId, role, now(), null);
    }

    /**
     * 获取新的领域 ID。
     *
     * @return 全局唯一长整型 ID
     */
    protected long nextId() {
        return idGenerator.nextLongId();
    }

    /**
     * 获取领域服务统一时间。
     *
     * @return 当前时间
     */
    protected Instant now() {
        return timeProvider.nowInstant();
    }

    /**
     * 校验 ID 或数值入参必须为正数。
     *
     * @param value 待校验数值
     * @param fieldName 字段名，用于生成校验失败消息
     */
    protected void requirePositive(long value, String fieldName) {
        channelCommandValidator.requirePositive(value, fieldName);
    }

    /**
     * 规范化治理原因文本。
     *
     * @param reason 原始原因文本
     * @return 规范化后的原因文本
     */
    protected String normalizeReason(String reason) {
        return channelCommandValidator.normalizeReason(reason);
    }

    /**
     * 规范化可为空的展示文本。
     *
     * @param value 原始文本
     * @return 去除首尾空白后的文本，空值按统一规则处理
     */
    protected String normalizeNullableText(String value) {
        return channelCommandValidator.normalizeNullableText(value);
    }

    /**
     * 构建封禁动作审计元数据。
     *
     * @param ban 封禁记录
     * @return 审计元数据 JSON 文本
     */
    protected String buildBanAuditMetadata(ChannelBan ban) {
        return channelCommandValidator.buildBanAuditMetadata(ban);
    }
}
