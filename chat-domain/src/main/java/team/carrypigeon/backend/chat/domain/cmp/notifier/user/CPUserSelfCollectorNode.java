package team.carrypigeon.backend.chat.domain.cmp.notifier.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;

import java.util.HashSet;
import java.util.Set;

/**
 * Collect current user id for notification.
 * <p>
 * Input:
 *  - SessionId:Long
 * Output:
 *  - Notifier_Uids:Set<Long> (append)
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPUserSelfCollector")
public class CPUserSelfCollectorNode extends CPNodeComponent {

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long uid = context.getData(CPNodeCommonKeys.SESSION_ID);
        if (uid == null) {
            log.error("CPUserSelfCollector args error: SessionId is null");
            argsError(context);
            return;
        }
        Set<Long> uids = context.getData(CPNodeNotifierKeys.NOTIFIER_UIDS);
        if (uids == null) {
            uids = new HashSet<>();
            context.setData(CPNodeNotifierKeys.NOTIFIER_UIDS, uids);
        }
        uids.add(uid);
        log.debug("CPUserSelfCollector collected uid={}, totalUids={}", uid, uids.size());
    }
}
