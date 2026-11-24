package team.carrypigeon.backend.chat.domain.features.channel.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 频道 feature 的错误码表。
 * 职责：完整声明频道生命周期、成员治理、禁言与入群申请相关的失败语义。
 * 边界：只服务频道 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum ChannelProblem implements ProblemMapping {

    /** 频道相关入参不合法。 */
    INVALID_INPUT(10001, "validation failed"),

    /** 分页游标格式非法。 */
    CURSOR_INVALID(10013, "cursor is invalid"),

    /** 目标频道不存在。 */
    CHANNEL_NOT_FOUND(20001, "channel does not exist"),

    /** 目标频道成员关系不存在。 */
    MEMBER_NOT_FOUND(20003, "channel member does not exist"),

    /** 目标禁言记录不存在。 */
    BAN_NOT_FOUND(20004, "channel ban does not exist"),

    /** 目标入群申请不存在，或不属于命令声明的频道。 */
    APPLICATION_NOT_FOUND(20005, "channel application does not exist"),

    /** 调用方没有该频道的成员关系。 */
    MEMBERSHIP_REQUIRED(20002, "channel membership is required"),

    /** 读取频道详情时调用方不是成员，与成员治理类拒绝保持不同原因。 */
    READ_FORBIDDEN(20002, "channel membership is required"),

    /** 该动作要求调用方是频道所有者。 */
    OWNER_REQUIRED(20006, "channel action requires owner role"),

    /** 角色变更动作要求调用方是频道所有者。 */
    ROLE_FORBIDDEN(20007, "channel action requires owner role"),

    /** 所有权转移要求调用方是频道所有者。 */
    OWNERSHIP_FORBIDDEN(20008, "channel action requires owner role"),

    /** 踢出成员要求调用方是所有者或管理员。 */
    KICK_FORBIDDEN(20009, "channel action requires owner or admin role"),

    /** 资料更新要求调用方是所有者或管理员。 */
    PROFILE_FORBIDDEN(20010, "channel action requires owner or admin role"),

    /** 禁言动作要求调用方是所有者或管理员。 */
    BAN_FORBIDDEN(20011, "channel action requires owner or admin role"),

    /** 查看申请名单要求调用方是所有者或管理员。 */
    APPLICATION_LIST_FORBIDDEN(20012, "channel invite requires owner or admin role"),

    /** 审批入群申请要求调用方是所有者或管理员。 */
    APPLICATION_DECIDE_FORBIDDEN(20013, "channel application requires owner or admin role"),

    /** 频道仍存在申请或禁言等依赖数据，不能删除。 */
    DELETE_BLOCKED(20014, "channel contains dependent data and cannot be deleted"),

    /** 治理写入未命中预期成员，整个事务必须回滚。 */
    GOVERNANCE_CONFLICT(20015, "channel membership changed; retry the action"),

    /** 入群申请已被处理，不能重复审批。 */
    APPLICATION_ALREADY_PROCESSED(20016, "channel application is already processed"),

    /** 申请人已经是频道成员，审批失去意义。 */
    APPLICANT_ALREADY_MEMBER(20017, "already a member");

    private final int code;
    private final String message;

    ChannelProblem(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
