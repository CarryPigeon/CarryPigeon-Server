package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用于获取用户所有通道的Node<br/>
 * 入参: UserInfo_Id:Long<br/>
 * 出参: channels:Set<CPChannel><br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelGroupSelector")
public class CPChannelGroupSelectorNode extends AbstractSelectorNode<Set> {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;

    /**
     * 读取当前节点的执行模式。
     *
     * @param context LiteFlow 上下文（此节点固定返回 all 模式）
     * @return 固定模式字符串 { all}
     */
    @Override
    protected String readMode(CPFlowContext context) {
        return "all";
    }

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（当前仅支持 { all}）
     * @param context LiteFlow 上下文（此节点固定返回 all 模式）
     * @return 当前用户可见的频道集合（固定频道+已加入频道）
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected Set doSelect(String mode, CPFlowContext context) throws Exception {
        Long userId = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
        CPChannel[] allFixedChannel = select(context,
                buildSelectKey("channel", "fixed", "all"),
                channelDao::getAllFixed);
        CPChannelMember[] allUserChannel = select(context,
                buildSelectKey("channel_member", "uid", userId),
                () -> channelMemberDao.getAllMemberByUserId(userId));
        Set<CPChannel> result = new HashSet<>(allFixedChannel.length + allUserChannel.length);
        Collections.addAll(result, allFixedChannel);
        for (CPChannelMember channelMember : allUserChannel) {
            long cid = channelMember.getCid();
            CPChannel channel = select(context,
                    buildSelectKey("channel", "id", cid),
                    () -> channelDao.getById(cid));
            if (channel != null) {
                result.add(channel);
            }
        }
        return result;
    }

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 频道列表写入键 { CHANNEL_INFO_LIST}
     */
    @Override
    protected CPKey<Set> getResultKey() {
        return CPNodeChannelKeys.CHANNEL_INFO_LIST;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式（当前仅支持 { all}）
     * @param context LiteFlow 上下文（此节点固定返回 all 模式）
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
    }
}
