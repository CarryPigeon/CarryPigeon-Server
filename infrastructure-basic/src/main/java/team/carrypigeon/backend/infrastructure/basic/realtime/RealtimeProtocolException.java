package team.carrypigeon.backend.infrastructure.basic.realtime;

/**
 * 实时协议约束被违反。
 * 职责：为基建内的协议校验提供稳定的失败原因与对外消息，供传输分派层转换为命令错误帧。
 * 边界：只表达"线上协议不合法"，不承载业务失败语义；不携带 HTTP 状态码。
 */
public final class RealtimeProtocolException extends RuntimeException {

    private final String reason;

    /**
     * 创建协议异常。
     *
     * @param reason 稳定失败原因，会原样下发到客户端的 error.reason
     * @param message 面向客户端的失败消息
     */
    public RealtimeProtocolException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * 稳定失败原因。
     *
     * @return 失败原因
     */
    public String reason() {
        return reason;
    }
}
