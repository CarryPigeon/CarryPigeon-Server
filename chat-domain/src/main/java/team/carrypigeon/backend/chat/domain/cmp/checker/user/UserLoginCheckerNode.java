package team.carrypigeon.backend.chat.domain.cmp.checker.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;

/**
 * 会话登录状态检查节点（从 {@code CPSession} 读取 uid 并写入上下文）。
 * <p>
 * bind 参数：
 * <ul>
 *     <li>{@code type=soft|hard}：默认 hard</li>
 * </ul>
 *
 * <p>输入：
 * <ul>
 *     <li>从 {@link CPSession} 读取 {@link CPChatDomainAttributes#CHAT_DOMAIN_USER_ID}</li>
 * </ul>
 *
 * <p>输出：
 * <ul>
 *     <li>{@link CPFlowKeys#SESSION_UID}：{@code Long} 当前登录用户 id</li>
 *     <li>soft 模式下还会写入 {@link CPFlowKeys#CHECK_RESULT}（{@link team.carrypigeon.backend.api.chat.domain.flow.CheckResult}）</li>
 * </ul>
 *
 * <p>失败行为：
 * <ul>
 *     <li>hard：抛出 {@code 401 unauthorized} 中断链路</li>
 *     <li>soft：仅写入 {@link team.carrypigeon.backend.api.chat.domain.flow.CheckResult}，不中断链路</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("UserLoginChecker")
public class UserLoginCheckerNode extends AbstractCheckerNode {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();

        Long attributeValue = session != null
                ? session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class)
                : null;
        if (attributeValue == null) {
            if (soft) {
                // 未登录：soft 模式下仅写入 CheckResult，不中断流程
                markSoftFail(context, "user not login");
                log.debug("UserLoginChecker soft fail: user not login");
                return;
            }
            // 未登录：hard 模式下直接抛出错误
            log.debug("UserLoginChecker hard fail: user not login");
            fail(CPProblem.of(401, "unauthorized", "user not login"));
        }
        // 写入 session_uid，供后续节点使用
        context.set(CPFlowKeys.SESSION_UID, attributeValue);
        if (soft) {
            markSoftSuccess(context);
            log.debug("UserLoginChecker soft success, uid={}", attributeValue);
        }
    }
}
