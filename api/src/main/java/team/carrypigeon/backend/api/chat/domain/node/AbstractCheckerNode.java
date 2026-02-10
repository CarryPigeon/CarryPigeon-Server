package team.carrypigeon.backend.api.chat.domain.node;

import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

/**
 * 业务校验节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 执行业务规则校验，支持 soft（软）和 hard（硬）两种模式：
 * <ul>
 *   <li>hard 模式：校验失败立即中断流程，抛出错误</li>
 *   <li>soft 模式：校验失败仅记录结果到上下文，不中断流程</li>
 * </ul>
 *
 * <h2>与 Guard 的区别</h2>
 * <ul>
 *   <li>{@link AbstractGuardNode}：只有 hard 模式，用于权限/前置校验</li>
 *   <li>AbstractCheckerNode：支持 soft/hard 模式，用于业务规则校验</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("CPChannelBanChecker")
 * public class CPChannelBanCheckerNode extends AbstractCheckerNode {
 *
 *     @Override
 *     protected void process(CPFlowContext context) {
 *         boolean soft = isSoftMode();
 *         Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
 *         Long uid = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
 *
 *         CPChannelBan ban = banDao.getByChannelIdAndUserId(cid, uid);
 *         if (ban != null && ban.isValid()) {
 *             if (soft) {
 *                 markSoftFail(context, "user_muted");
 *                 return;
 *             }
 *             forbidden(CPProblemReason.USER_MUTED, "you are muted in this channel");
 *         }
 *
 *         if (soft) {
 *             markSoftSuccess(context);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>XML 配置示例</h2>
 * <pre>{@code
 * <!-- hard 模式（默认） -->
 * CPChannelBanChecker
 *
 * <!-- soft 模式 -->
 * CPChannelBanChecker.bind("type","soft")
 * }</pre>
 *
 * @see AbstractGuardNode
 * @see CheckResult
 */
public abstract class AbstractCheckerNode extends CPNodeComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractCheckerNode.class);

    /**
     * 判断是否为 soft 模式。
     * <p>
     * 从 bind 参数 "type" 读取：
     * <ul>
     *   <li>"soft" → true</li>
     *   <li>其他值或未配置 → false（hard 模式）</li>
     * </ul>
     *
     * @return true=soft 模式, false=hard 模式
     */
    protected boolean isSoftMode() {
        String type = getBindData(CPNodeBindKeys.TYPE, String.class);
        return "soft".equalsIgnoreCase(type);
    }

    /**
     * 写入校验结果到上下文。
     * <p>
     * 仅在 soft 模式下使用，hard 模式直接抛出异常。
     *
     * @param context 上下文
     * @param success 校验是否通过
     * @param message 失败原因（成功时可为 null）
     */
    protected void setCheckResult(CPFlowContext context, boolean success, String message) {
        CheckResult result = new CheckResult(success, message);
        context.set(CPFlowKeys.CHECK_RESULT, result);
        log.debug("[{}] 软校验结果: success={}, message={}", getNodeId(), success, message);
    }

    /**
     * 标记软校验成功。
     * <p>
     * 等同于 {@code setCheckResult(context, true, null)}
     *
     * @param context 上下文
     */
    protected void markSoftSuccess(CPFlowContext context) {
        setCheckResult(context, true, null);
    }

    /**
     * 标记软校验失败。
     * <p>
     * 等同于 {@code setCheckResult(context, false, message)}
     *
     * @param context 上下文
     * @param message 失败原因
     */
    protected void markSoftFail(CPFlowContext context, String message) {
        setCheckResult(context, false, message);
    }
}
