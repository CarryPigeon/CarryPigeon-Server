package team.carrypigeon.backend.chat.domain.features.message.domain.problem;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * 消息 feature 的错误码表。
 * 职责：完整声明消息发送、撤回、提及与 domain 支持相关的失败语义。
 * 边界：只服务消息 feature，不引用其它 feature 的码表；与其它 feature 语义相同的码各自独立定义。
 */
public enum MessageProblem implements ProblemMapping {

    /** 消息相关入参不合法。 */
    INVALID_INPUT(422, "validation_failed", "validation failed"),

    /** 分页游标格式非法。 */
    CURSOR_INVALID(422, "cursor_invalid", "cursor is invalid"),

    /** 目标消息不存在。 */
    MESSAGE_NOT_FOUND(404, "not_found", "message does not exist"),

    /** 目标提及记录不存在。 */
    MENTION_NOT_FOUND(404, "not_found", "mention not found"),

    /** 当前成员处于禁言状态，不能投递消息。 */
    USER_MUTED(403, "user_muted", "channel member is muted"),

    /** 调用方无权撤回目标消息。 */
    RECALL_FORBIDDEN(403, "channel_message_recall_forbidden", "channel message recall is not allowed"),

    /** 当前运行时没有该消息 domain 的处理器。 */
    UNSUPPORTED_DOMAIN(422, "unsupported_message_domain", "message domain is not supported");

    private final int status;
    private final String reason;
    private final String message;

    MessageProblem(int status, String reason, String message) {
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
