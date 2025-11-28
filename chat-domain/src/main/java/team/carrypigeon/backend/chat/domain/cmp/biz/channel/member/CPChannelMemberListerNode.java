package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

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
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_CID);
        if (cid == null){
            argsError(context);
            return;
        }
        CPChannelMember[] allMember = channelMemberDao.getAllMember(cid);
        HashSet<CPChannelMember> objects = new HashSet<>();
        for (CPChannelMember member : allMember) {
            objects.add(member);
        }
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_LIST, objects);
    }
}
