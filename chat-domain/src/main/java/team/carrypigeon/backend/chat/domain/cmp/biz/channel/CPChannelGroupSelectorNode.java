package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

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
public class CPChannelGroupSelectorNode extends AbstractSelectorNode<java.util.List<CPChannel>> {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;

    @Override
    protected String readMode(DefaultContext context) {
        // 不依赖 bind，固定为 all
        return "all";
    }

    @Override
    protected List<CPChannel> doSelect(String mode, DefaultContext context) throws Exception {
        Long userId = requireContext(context, CPNodeUserKeys.USER_INFO_ID, Long.class);
        CPChannel[] allFixedChannel = channelDao.getAllFixed();
        CPChannelMember[] allUserChannel = channelMemberDao.getAllMemberByUserId(userId);
        List<CPChannel> result = new ArrayList<>(allFixedChannel.length + allUserChannel.length);
        Collections.addAll(result, allFixedChannel);
        for (CPChannelMember channelMember : allUserChannel) {
            result.add(channelDao.getById(channelMember.getCid()));
        }
        return result;
    }

    @Override
    protected String getResultKey() {
        return CPNodeChannelKeys.CHANNEL_INFO_LIST;
    }

    @Override
    protected void handleNotFound(String mode, DefaultContext context) {
        // 理论上不会为 null，保持空实现即可
    }
}
