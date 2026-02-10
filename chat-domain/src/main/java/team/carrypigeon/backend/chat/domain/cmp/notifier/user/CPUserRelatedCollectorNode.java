package team.carrypigeon.backend.chat.domain.cmp.notifier.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户相关用户的数据收集节点<br/>
 * 入参：UserInfo_Id:{@link CPUser}<br/>
 * 出参：Notifier_Uids:Set<Long>(追加)<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserRelatedCollector")
public class CPUserRelatedCollectorNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;


    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前调用会话（仅用于链路签名）
     * @param context LiteFlow 上下文，读取用户与频道成员关系并汇总通知目标
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long userInfoId = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
        Set<Long> uids = context.get(CPNodeNotifierKeys.NOTIFIER_UIDS);
        if (uids == null){
            uids = new HashSet<>();
            context.set(CPNodeNotifierKeys.NOTIFIER_UIDS, uids);
        }
        CPChannelMember[] allMemberByUserId = select(context,
                buildSelectKey("channel_member", "uid", userInfoId),
                () -> channelMemberDao.getAllMemberByUserId(userInfoId));
        for (CPChannelMember cpChannelMember : allMemberByUserId) {
            long cid = cpChannelMember.getCid();
            CPChannelMember[] allMember = select(context,
                    buildSelectKey("channel_member", "cid", cid),
                    () -> channelMemberDao.getAllMember(cid));
            for (CPChannelMember member : allMember) {
                uids.add(member.getUid());
            }
        }
    }
}
