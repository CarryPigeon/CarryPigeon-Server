package team.carrypigeon.backend.infrastructure.basic.logging;

/**
 * 日志作用域。
 * 职责：标识当前日志来自 HTTP 请求还是 WebSocket 帧，便于统一日志按通道检索。
 * 边界：只描述日志上下文来源，不参与协议处理或业务判断。
 */
public enum LogScope {

    /** HTTP 请求。 */
    HTTP("http"),

    /** WebSocket 帧。 */
    WS("ws");

    private final String wireName;

    LogScope(String wireName) {
        this.wireName = wireName;
    }

    /**
     * 返回写入日志上下文的稳定取值。
     *
     * @return 日志作用域名称
     */
    public String wireName() {
        return wireName;
    }
}
