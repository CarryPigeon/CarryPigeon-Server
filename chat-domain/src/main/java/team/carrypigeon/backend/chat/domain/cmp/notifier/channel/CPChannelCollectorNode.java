package team.carrypigeon.backend.chat.domain.cmp.notifier.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import java.util.Arrays;
import java.util.HashSet;

/**
 * 通道数据获取组件<br/>
 * 获取一个用户所在的所有通道数据，并保存到上下文中。<br/>
 * 入参：UserInfo_Id:Long<br/>
 * 出参：无
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelCollector")
public class CPChannelCollectorNode extends CPNodeComponent {


    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long uid = context.getData(CPNodeUserKeys.USER_INFO_ID);
        if (uid == null){
            argsError( context);
            return;
        }
        CPChannelMember[] members = channelMemberDao.getAllMemberByUserId(uid);
        CPChannel[] allFixed = channelDao.getAllFixed();
        HashSet<CPChannel> cpChannels = new HashSet<>();
        for (CPChannelMember member : members) {
            cpChannels.add(channelDao.getById(member.getCid()));
        }
        cpChannels.addAll(Arrays.asList(allFixed));
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_LIST,cpChannels);
        log.debug("CPChannelCollector success, uid={},size={}", uid,cpChannels.size());
    }
}
