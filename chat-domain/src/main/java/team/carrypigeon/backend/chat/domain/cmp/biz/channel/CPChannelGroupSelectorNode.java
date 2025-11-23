package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用于获取用户所有通道的Node<br/>
 * 入参: UserInfo_Id:Long<br/>
 * 出参: channels:Set<CPChannel><br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelGroupSelector")
public class CPChannelGroupSelectorNode extends CPNodeComponent {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long userId = context.getData("UserInfo_Id");
        if (userId == null){
            argsError(context);
        }
        CPChannel[] allFixedChannel = channelDao.getAllFixed();
        CPChannelMember[] allUserChannel = channelMemberDao.getAllMemberByUserId(userId);
        List<CPChannel> result = new ArrayList<>(allFixedChannel.length+allUserChannel.length);
        Collections.addAll(result, allFixedChannel);
        for (CPChannelMember channelMember : allUserChannel) {
            result.add(channelDao.getById(channelMember.getCid()));
        }
        context.setData("channels",result);
    }
}
