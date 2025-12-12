package team.carrypigeon.backend.chat.domain.attribute;

/**
 * LiteFlow 节点中使用的 bind 参数 key 常量。
 * <p>
 * 目前通用约定：
 * <ul>
 *   <li>{@link #KEY}：大多数节点用于传递模式 / 子类型，例如 selector 模式；</li>
 *   <li>{@link #TYPE}：checker / parser 节点的模式（soft / hard 等）；</li>
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

    private CPNodeBindKeys() {
    }
}

