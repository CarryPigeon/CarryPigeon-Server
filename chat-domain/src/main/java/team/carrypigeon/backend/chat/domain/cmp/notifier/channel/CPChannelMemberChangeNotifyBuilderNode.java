package team.carrypigeon.backend.chat.domain.cmp.notifier.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.connection.notification.CPChannelMemberNotificationData;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;

/**
 * 构建频道成员变动推送数据的节点。<br/>
 * bind:
 *  - key=join         表示有新成员加入
 *  - key=leave        表示成员被移除
 *  - key=admin_add    表示成员被设为管理员
 *  - key=admin_remove 表示成员被取消管理员
 * <br/>
 * 入参：
 *  - ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参：
 *  - Notifier_Data:JsonNode（CPChannelMemberNotificationData 序列化结果）<br/>
 * 该节点只负责构建推送 data，不负责确定通知的目标用户。
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberChangeNotifyBuilder")
public class CPChannelMemberChangeNotifyBuilderNode extends CPNodeComponent {

    private final ObjectMapper objectMapper;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        // 变更类型由 bind("key", ...) 指定
        String type = requireBind(context, CPNodeBindKeys.KEY, String.class);
        CPChannelMember member =
                requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, CPChannelMember.class);

        CPChannelMemberNotificationData data = new CPChannelMemberNotificationData()
                .setType(type)
                .setCid(member.getCid())
                .setUid(member.getUid());

        context.setData(CPNodeNotifierKeys.NOTIFIER_DATA, objectMapper.valueToTree(data));
        log.debug("CPChannelMemberChangeNotifyBuilder built notify data, type={}, cid={}, uid={}",
                type, member.getCid(), member.getUid());
    }
}
