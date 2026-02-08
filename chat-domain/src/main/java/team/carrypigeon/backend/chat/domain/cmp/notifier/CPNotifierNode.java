package team.carrypigeon.backend.chat.domain.cmp.notifier;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeBindKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.notification.CPNotificationSender;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;

import java.util.Set;

/**
 * 通知发送节点（统一推送出口）。
 * <p>
 * bind 参数：
 * <ul>
 *     <li>{@code route}：通知路由（写入 {@link CPNotification#setRoute(String)}）</li>
 * </ul>
 *
 * <p>入参（context）：
 * <ul>
 *     <li>{@link CPNodeNotifierKeys#NOTIFIER_UIDS}：{@code Set<Long>} 需要通知的 uid 集合</li>
 *     <li>{@link CPNodeNotifierKeys#NOTIFIER_DATA}：{@code JsonNode} 通知 payload</li>
 * </ul>
 *
 * <p>出参：无（副作用：向在线会话写出 {@code CPResponse(id=-1, code=0)} 推送）。</p>
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPNotifier")
public class CPNotifierNode extends CPNodeComponent {

    private final CPNotificationSender notificationSender;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        String route = requireBind(CPNodeBindKeys.ROUTE, String.class);
        Set<Long> uids = requireContext(context, CPNodeNotifierKeys.NOTIFIER_UIDS);
        CPNotification notification = new CPNotification();
        notification.setRoute(route)
                .setData(context.get(CPNodeNotifierKeys.NOTIFIER_DATA));
        log.debug("CPNotifier: send notification, route={}, uidsCount={}", route, uids.size());
        notificationSender.sendNotification(uids, notification);
    }
}
