package team.carrypigeon.backend.chat.domain.cmp.notifier.user;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;

import java.util.HashSet;
import java.util.Set;

/**
 * Collect current user id for notification.
 * <p>
 * Input:
 *  - session_uid:Long
 * Output:
 *  - Notifier_Uids:Set<Long> (append)
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPUserSelfCollector")
public class CPUserSelfCollectorNode extends CPNodeComponent {

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前调用会话（本节点不直接写回）
     * @param context LiteFlow 上下文，读取会话用户并维护通知目标集合
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long uid = requireContext(context, CPFlowKeys.SESSION_UID);
        Set<Long> uids = context.get(CPNodeNotifierKeys.NOTIFIER_UIDS);
        if (uids == null) {
            uids = new HashSet<>();
            context.set(CPNodeNotifierKeys.NOTIFIER_UIDS, uids);
        }
        uids.add(uid);
        log.debug("CPUserSelfCollector collected uid={}, totalUids={}", uid, uids.size());
    }
}
