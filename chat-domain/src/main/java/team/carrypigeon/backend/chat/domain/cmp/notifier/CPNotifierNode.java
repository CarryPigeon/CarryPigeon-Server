package team.carrypigeon.backend.chat.domain.cmp.notifier;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;

import java.util.Set;

/**
 * 通知者节点<br/>
 * 入参：<br/>
 * 1. Notifier_Uids:Set<Long> <br/>
 * 2. Notifier_Data:JsonNode?(可通过前置的result来进行构建数据)<br/>
 * 出参：无
 * */
@AllArgsConstructor
@LiteflowComponent("CPNotifier")
public class CPNotifierNode extends CPNodeComponent {

    private final CPNotificationService cpNotificationService;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String route = getBindData("route", String.class);
        if (route == null){
            argsError(context);
        }
        Set<Long> uids = context.getData("Notifier_Uids");
        if (route == null || uids == null){
            argsError(context);
        }
        CPNotification notification = new CPNotification();
        notification.setRoute(route)
                .setData(context.getData("Notifier_Data"));
        assert uids != null;
        cpNotificationService.sendNotification(uids, notification);
    }
}
