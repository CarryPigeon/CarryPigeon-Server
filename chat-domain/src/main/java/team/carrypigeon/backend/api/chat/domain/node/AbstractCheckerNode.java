package team.carrypigeon.backend.api.chat.domain.node;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 通用的“软/硬”校验节点基类。
 * <p>
 * 封装：
 * <ul>
 *     <li>bind 参数 {@code type=soft|hard} 的解析逻辑；</li>
 *     <li>将校验结果写入 {@link CheckResult} 的通用方法。</li>
 * </ul>
 * 具体业务校验节点只需关心自身的校验条件与日志输出。
 */
public abstract class AbstractCheckerNode extends CPNodeComponent {

    /**
     * 是否以 soft 模式执行：
     * <pre>
     * bind("type","soft")  => true
     * 其他值或未配置        => false (hard 模式)
     * </pre>
     */
    protected boolean isSoftMode() {
        String type = getBindData(CPNodeBindKeys.TYPE, String.class);
        return "soft".equalsIgnoreCase(type);
    }

    /**
     * 写入 {@link CheckResult}。
     *
     * @param context LiteFlow 上下文
     * @param success 校验是否通过
     * @param message 失败原因（成功时可以为 null）
     */
    protected void setCheckResult(CPFlowContext context, boolean success, String message) {
        context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(success, message));
    }

    /**
     * 软校验成功时的快捷方法。
     */
    protected void markSoftSuccess(CPFlowContext context) {
        setCheckResult(context, true, null);
    }

    /**
     * 软校验失败时的快捷方法。
     */
    protected void markSoftFail(CPFlowContext context, String message) {
        setCheckResult(context, false, message);
    }
}
