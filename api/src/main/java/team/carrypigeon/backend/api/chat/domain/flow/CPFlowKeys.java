package team.carrypigeon.backend.api.chat.domain.flow;

/**
 * LiteFlow 上下文 Key 常量定义。
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>所有跨节点共享的数据必须通过此类定义的 Key 存取</li>
 *   <li>禁止在代码中使用魔法字符串作为 Key</li>
 *   <li>业务域专用 Key 定义在 {@code CPNode*Keys} 中</li>
 * </ul>
 *
 * <h2>Key 命名规范</h2>
 * <ul>
 *   <li>基础 Key：小写下划线（如 {@code request}, {@code auth_uid}）</li>
 *   <li>业务 Key：{@code Entity_Field} 格式（如 {@code UserInfo_Id}）</li>
 * </ul>
 *
 * <h2>使用场景</h2>
 * <pre>
 * Controller:  context.setData(REQUEST, dto)
 * BindNode:    context.getData(REQUEST) → context.setData(USER_INFO_ID, ...)
 * ResultNode:  context.getData(...) → context.setData(RESPONSE, result)
 * </pre>
 *
 * @see CPFlowContext
 */
public final class CPFlowKeys {

    // ==================== 请求/响应 ====================

    /**
     * 请求 DTO 对象。
     * <p>
     * 由 Controller 从 HTTP 请求体反序列化后写入，
     * 由 Bind 节点读取并解析到业务 Key。
     * <p>
     * 类型：取决于具体接口的 Request DTO
     */
    public static final CPKey<Object> REQUEST = CPKey.of("request", Object.class);

    /**
     * 响应 DTO 对象。
     * <p>
     * 由 Result 节点构建后写入，
     * 由 Controller 读取并序列化为 HTTP 响应。
     * <p>
     * 类型：取决于具体接口的 Response DTO
     */
    public static final CPKey<Object> RESPONSE = CPKey.of("response", Object.class);

    // ==================== 认证信息 ====================

    /**
     * 已认证用户 ID。
     * <p>
     * 由 Controller 从 HTTP Filter 设置的 Request Attribute 中读取后写入，
     * 由 LoginGuard 节点校验并传递给后续节点。
     * <p>
     * 类型：{@code Long}
     */
    public static final CPKey<Long> AUTH_UID = CPKey.of("auth_uid", Long.class);

    /**
     * 当前会话用户 ID（登录校验通过后写入）。
     * <p>
     * 由 LoginGuard 节点校验 {@link #AUTH_UID} 后写入，
     * 供后续业务节点使用（等同于"当前登录用户"）。
     * <p>
     * 类型：{@code Long}
     */
    public static final CPKey<Long> SESSION_UID = CPKey.of("session_uid", Long.class);

    // ==================== 校验结果 ====================

    /**
     * 软校验结果。
     * <p>
     * 由 Checker 节点在 soft 模式下写入，
     * 用于条件分支判断（不中断流程）。
     * <p>
     * 类型：{@code CheckResult}
     *
     * @see CheckResult
     */
    public static final CPKey<CheckResult> CHECK_RESULT = CPKey.of("check_result", CheckResult.class);

    private CPFlowKeys() {
        // 禁止实例化
    }
}
