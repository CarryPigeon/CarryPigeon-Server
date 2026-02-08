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
