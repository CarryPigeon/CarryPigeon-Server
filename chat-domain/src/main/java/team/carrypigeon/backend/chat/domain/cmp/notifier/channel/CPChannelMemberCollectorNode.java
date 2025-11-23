package team.carrypigeon.backend.chat.domain.cmp.notifier.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;

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
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannel channelInfo = context.getData("ChannelInfo");
        Set<Long> uids = context.getData("Notifier_Uids");
        if (channelInfo==null){
            argsError(context);
        }
        if (uids==null){
            uids = new HashSet<>();
            context.setData("Notifier_Uids",uids);
        }
        CPChannelMember[] allMember = channelMemberDao.getAllMember(channelInfo.getId());
        Arrays.stream(allMember).map(CPChannelMember::getUid).forEach(uids::add);
    }
}
