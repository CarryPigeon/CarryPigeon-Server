package team.carrypigeon.backend.chat.domain.cmp.notifier.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
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

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param context LiteFlow 上下文，读取频道信息并收集关联用户 ID
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        Long uid = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
        CPChannelMember[] members = select(context,
                buildSelectKey("channel_member", "uid", uid),
                () -> channelMemberDao.getAllMemberByUserId(uid));
        CPChannel[] allFixed = select(context,
                buildSelectKey("channel", "fixed", "all"),
                channelDao::getAllFixed);
        HashSet<CPChannel> cpChannels = new HashSet<>();
        for (CPChannelMember member : members) {
            long cid = member.getCid();
            CPChannel channel = select(context,
                    buildSelectKey("channel", "id", cid),
                    () -> channelDao.getById(cid));
            if (channel != null) {
                cpChannels.add(channel);
            }
        }
        cpChannels.addAll(Arrays.asList(allFixed));
        context.set(CPNodeChannelKeys.CHANNEL_INFO_LIST,cpChannels);
        log.debug("CPChannelCollector success, uid={},size={}", uid,cpChannels.size());
    }
}
