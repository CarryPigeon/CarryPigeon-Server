package team.carrypigeon.backend.api.chat.domain.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 权限守卫（Guard）节点基类。
 *
 * <h2>职责</h2>
 * <p>
 * 执行权限/前置条件校验，校验失败时立即中断流程并返回错误响应。
 * 与 {@link AbstractCheckerNode} 的区别：Guard 只有 hard 模式，不支持 soft 模式。
 *
 * <h2>处理流程</h2>
 * <pre>
 * 1. 调用 {@link #check(CPFlowContext)} 执行校验
 * 2. 校验失败：调用 {@link #fail(CPProblem)} 抛出错误
 * 3. 校验成功：流程继续
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @LiteflowComponent("MemberGuard")
 * public class MemberGuardNode extends AbstractGuardNode {
 *
 *     private final ChannelMemberDao memberDao;
 *
 *     @Override
 *     protected boolean check(CPFlowContext context) {
 *         Long cid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
 *         Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
 *         return memberDao.existsByCidAndUid(cid, uid);
 *     }
 *
 *     @Override
 *     protected CPProblem getProblem() {
 *         return CPProblem.of(CPProblemReason.NOT_CHANNEL_MEMBER, "you are not a member of this channel");
 *     }
 * }
 * }</pre>
 *
 * <h2>常用 Guard 类型</h2>
 * <ul>
 *   <li>LoginGuard：认证守卫，校验用户是否已登录</li>
 *   <li>MemberGuard：成员守卫，校验用户是否为频道成员</li>
 *   <li>AdminGuard：管理员守卫，校验用户是否为频道管理员</li>
 *   <li>OwnerGuard：所有者守卫，校验用户是否为频道所有者</li>
 * </ul>
 *
 * @see AbstractCheckerNode
 */
public abstract class AbstractGuardNode extends CPNodeComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractGuardNode.class);

    /**
     * 执行守卫节点主流程：检查条件并在失败时中断链路。
     *
     * @param context 链路上下文
     * @throws Exception 校验过程中的异常
     */
    @Override
    protected final void process(CPFlowContext context) throws Exception {
        boolean passed = check(context);
        if (!passed) {
            CPProblem problem = getProblem();
            log.info("[{}] 守卫校验失败: reason={}, message={}",
                    getNodeId(), problem.reason(), problem.message());
            fail(problem);
        }
        log.debug("[{}] 守卫校验通过", getNodeId());
    }

    /**
     * 执行校验逻辑。
     * <p>
     * 子类实现具体的校验逻辑，返回 true 表示通过，false 表示失败。
     *
     * <h3>实现规范</h3>
     * <ul>
     *   <li>使用 {@link #requireContext} 读取校验所需的参数</li>
     *   <li>只做校验，不修改上下文数据</li>
     *   <li>校验逻辑应简单明确，复杂校验使用 Checker 节点</li>
     * </ul>
     *
     * @param context 上下文
     * @return true=通过, false=失败
     * @throws Exception 校验过程中的异常
     */
    protected abstract boolean check(CPFlowContext context) throws Exception;

    /**
     * 获取校验失败时的错误信息。
     * <p>
     * 子类必须实现此方法，返回校验失败时的标准化错误。
     *
     * <h3>常用错误</h3>
     * <ul>
     *   <li>401 unauthorized：未认证</li>
     *   <li>403 forbidden/not_channel_member：权限不足</li>
     *   <li>403 not_channel_admin：需要管理员权限</li>
     *   <li>403 not_channel_owner：需要所有者权限</li>
     * </ul>
     *
     * @return 错误信息
     */
    protected abstract CPProblem getProblem();
}
