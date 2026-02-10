package team.carrypigeon.backend.chat.domain.cmp.notifier.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 用于收集频道成员的Node<br/>
 * 入参: ChannelInfo:{@link CPChannel}<br/>
 * 出参：Notifier_Uids:Set<Long>(追加)<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberCollector")
public class CPChannelMemberCollectorNode extends CPNodeComponent {
    private final ChannelMemberDao channelMemberDao;
    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前调用会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取频道成员并合并到通知接收集合
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        CPChannel channelInfo = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO);
        Set<Long> uids = context.get(CPNodeNotifierKeys.NOTIFIER_UIDS);
        if (uids == null){
            uids = new HashSet<>();
            context.set(CPNodeNotifierKeys.NOTIFIER_UIDS, uids);
        }
        long cid = channelInfo.getId();
        CPChannelMember[] allMember = select(context,
                buildSelectKey("channel_member", "cid", cid),
                () -> channelMemberDao.getAllMember(cid));
        Arrays.stream(allMember).map(CPChannelMember::getUid).forEach(uids::add);
    }
}
