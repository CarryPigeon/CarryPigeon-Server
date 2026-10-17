package example.plugin.messageplugin.mcbridge;

import team.carrypigeon.backend.infrastructure.basic.exception.ProblemMapping;

/**
 * mc-bridge 插件自己的错误码表。
 * 职责：演示外部插件如何在不修改宿主任何码表的前提下声明自己的失败语义。
 * 边界：只服务本插件，不引用宿主或其它插件的码表。
 */
public enum McBridgeProblem implements ProblemMapping {

    /** payload 字段不是 JSON 对象。 */
    PAYLOAD_NOT_OBJECT(422, "validation_failed", "mc-bridge payload must be object"),

    /** event 字段缺失或为空白。 */
    EVENT_BLANK(422, "validation_failed", "mc-bridge event must not be blank");

    private final int status;
    private final String reason;
    private final String message;

    McBridgeProblem(int status, String reason, String message) {
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
