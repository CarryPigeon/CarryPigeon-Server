package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

import java.util.HashSet;

/**
 * 频道成员列表获取Node<br/>
 * 入参：<br/>
 * ChannelMemberInfo_Cid:Long<br/>
 * 出参：members:Set<CPChannelMember><br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberLister")
public class CPChannelMemberListerNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long cid = requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID);
        CPChannelMember[] allMember = select(context,
                buildSelectKey("channel_member", "cid", cid),
                () -> channelMemberDao.getAllMember(cid));
        HashSet<CPChannelMember> objects = new HashSet<>();
        for (CPChannelMember member : allMember) {
            objects.add(member);
        }
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_LIST, objects);
    }
}
