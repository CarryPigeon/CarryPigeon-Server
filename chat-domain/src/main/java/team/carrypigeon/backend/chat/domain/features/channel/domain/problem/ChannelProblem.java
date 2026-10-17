package team.carrypigeon.backend.chat.domain.features.channel.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 频道 feature 的错误码表。
 * 职责：完整声明频道生命周期、成员治理、禁言与入群申请相关的失败语义。
 * 边界：只服务频道 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum ChannelProblem implements ProblemMapping {

    /** 频道相关入参不合法。 */
    INVALID_INPUT(422, "validation_failed", "validation failed"),

    /** 分页游标格式非法。 */
    CURSOR_INVALID(422, "cursor_invalid", "cursor is invalid"),

    /** 目标频道不存在。 */
    CHANNEL_NOT_FOUND(404, "not_found", "channel does not exist"),

    /** 目标频道成员关系不存在。 */
    MEMBER_NOT_FOUND(404, "not_found", "channel member does not exist"),

    /** 目标禁言记录不存在。 */
    BAN_NOT_FOUND(404, "not_found", "channel ban does not exist"),

    /** 目标入群申请不存在，或不属于命令声明的频道。 */
    APPLICATION_NOT_FOUND(404, "not_found", "channel application does not exist"),

    /** 调用方没有该频道的成员关系。 */
    MEMBERSHIP_REQUIRED(403, "not_channel_member", "channel membership is required"),

    /** 读取频道详情时调用方不是成员，与成员治理类拒绝保持不同原因。 */
    READ_FORBIDDEN(403, "channel_membership_required", "channel membership is required"),

    /** 该动作要求调用方是频道所有者。 */
    OWNER_REQUIRED(403, "channel_owner_required", "channel action requires owner role"),

    /** 角色变更动作要求调用方是频道所有者。 */
    ROLE_FORBIDDEN(403, "channel_role_forbidden", "channel action requires owner role"),

    /** 所有权转移要求调用方是频道所有者。 */
    OWNERSHIP_FORBIDDEN(403, "channel_ownership_forbidden", "channel action requires owner role"),

    /** 踢出成员要求调用方是所有者或管理员。 */
    KICK_FORBIDDEN(403, "channel_kick_forbidden", "channel action requires owner or admin role"),

    /** 资料更新要求调用方是所有者或管理员。 */
    PROFILE_FORBIDDEN(403, "channel_profile_forbidden", "channel action requires owner or admin role"),

    /** 禁言动作要求调用方是所有者或管理员。 */
    BAN_FORBIDDEN(403, "channel_ban_forbidden", "channel action requires owner or admin role"),

    /** 查看申请名单要求调用方是所有者或管理员。 */
    APPLICATION_LIST_FORBIDDEN(403, "channel_invite_forbidden", "channel invite requires owner or admin role"),

    /** 审批入群申请要求调用方是所有者或管理员。 */
    APPLICATION_DECIDE_FORBIDDEN(403, "channel_invite_forbidden", "channel application requires owner or admin role"),

    /** 频道仍存在申请或禁言等依赖数据，不能删除。 */
    DELETE_BLOCKED(409, "channel_delete_blocked", "channel contains dependent data and cannot be deleted"),

    /** 入群申请已被处理，不能重复审批。 */
    APPLICATION_ALREADY_PROCESSED(409, "application_already_processed", "channel application is already processed"),

    /** 申请人已经是频道成员，审批失去意义。 */
    APPLICANT_ALREADY_MEMBER(409, "conflict", "already a member");

    private final int status;
    private final String reason;
    private final String message;

    ChannelProblem(int status, String reason, String message) {
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public String message() {
        return message;
    }
}
