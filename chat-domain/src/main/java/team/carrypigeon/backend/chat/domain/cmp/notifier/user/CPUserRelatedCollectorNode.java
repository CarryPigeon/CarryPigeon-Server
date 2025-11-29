package team.carrypigeon.backend.chat.domain.cmp.notifier.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

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


    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        // 获取用户信息
        Long userInfoId = context.getData(CPNodeValueKeyBasicConstants.USER_INFO_ID);
        if (userInfoId == null){
            argsError(context);
        }
        // 获取Set<Long>
        Set<Long> uids = context.getData(CPNodeValueKeyBasicConstants.NOTIFIER_UIDS);
        if (uids == null){
            uids = new HashSet<>();
            context.setData(CPNodeValueKeyBasicConstants.NOTIFIER_UIDS, uids);
        }
        // 获取用户id
        CPChannelMember[] allMemberByUserId = channelMemberDao.getAllMemberByUserId(userInfoId);
        for (CPChannelMember cpChannelMember : allMemberByUserId) {
            CPChannelMember[] allMember = channelMemberDao.getAllMember(cpChannelMember.getCid());
            for (CPChannelMember member : allMember) {
                uids.add(member.getUid());
            }
        }
    }
}
