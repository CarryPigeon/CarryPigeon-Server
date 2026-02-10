package team.carrypigeon.backend.api.chat.domain.node;

/**
 * LiteFlow 节点 bind 参数 key 常量。
 * <p>
 * 约定：
 * <ul>
 *   <li>{@link #KEY}：多数节点用于传递模式/子类型（如 selector 模式）；</li>
 *   <li>{@link #TYPE}：checker / parser 等节点的行为模式（soft / hard 等）；</li>
 *   <li>{@link #ROUTE}：通知节点用于指定推送路由。</li>
 * </ul>
 */
public final class CPNodeBindKeys {

    /** 通用模式 key，例如 selector 的查询模式。 */
    public static final String KEY = "key";

    /** 行为模式 key，例如 checker 或 parser 的硬/软检查模式。 */
    public static final String TYPE = "type";

    /** 通知或路由相关节点使用的路由 key。 */
    public static final String ROUTE = "route";

    /**
     * 工具类不允许实例化。
     */
    private CPNodeBindKeys() {
    }
}

