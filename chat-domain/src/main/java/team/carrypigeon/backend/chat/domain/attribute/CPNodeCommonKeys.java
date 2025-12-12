package team.carrypigeon.backend.chat.domain.attribute;

/**
 * LiteFlow 上下文中通用的基础 key 封装。
 * <p>
 * 这些字段在多个业务域中都会使用，
 * 本类作为这些 key 的实际定义处，并对外提供语义化分组。
 */
public final class CPNodeCommonKeys {

    /** CPSession: 当前会话对象 */
    public static final String SESSION = "session";

    /** Long: 当前登录用户的 id */
    public static final String SESSION_ID = "SessionId";

    /** CheckResult: 最近一次软失败检查结果 */
    public static final String CHECK_RESULT = "CheckResult";

    /** CPResponse: 最终返回给客户端的响应 */
    public static final String RESPONSE = "response";

    private CPNodeCommonKeys() {
    }
}
