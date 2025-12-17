package team.carrypigeon.backend.chat.domain.cmp.notifier;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.notification.CPNotificationSender;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;

import java.util.Set;

/**
 * 通知者节点<br/>
 * 入参：<br/>
 * 1. Notifier_Uids:Set<Long> <br/>
 * 2. Notifier_Data:JsonNode?(可通过前置的result来进行构建数据)<br/>
 * 出参：无
 * */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPNotifier")
public class CPNotifierNode extends CPNodeComponent {

    private final CPNotificationSender notificationSender;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        String route = requireBind(context, CPNodeBindKeys.ROUTE, String.class);
        Set<Long> uids = requireContext(context, CPNodeNotifierKeys.NOTIFIER_UIDS, Set.class);
        CPNotification notification = new CPNotification();
        notification.setRoute(route)
                .setData(context.getData(CPNodeNotifierKeys.NOTIFIER_DATA));
        log.debug("CPNotifier: send notification, route={}, uidsCount={}", route, uids.size());
        notificationSender.sendNotification(uids, notification);
    }
}
