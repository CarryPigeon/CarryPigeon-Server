package team.carrypigeon.backend.chat.domain.cmp.notifier.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;

import java.util.HashSet;
import java.util.Set;

/**
 * 频道管理员收集器<br/>
 * 入参：ChannelInfo:{@link CPChannel}<br/>
 * 出参：Notifier_Uids:Set<Long>(追加)<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelAdminCollector")
public class CPChannelAdminCollector extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannel channel = context.getData("ChannelInfo");
        if (channel==null){
            argsError(context);
            return;
        }
        Set<Long> uids = context.getData("Notifier_Uids");
        if (uids == null){
            uids = new HashSet<>();
            context.setData("Notifier_Uids",uids);
        }
        CPChannelMember[] allMember = channelMemberDao.getAllMember(channel.getId());
        for (CPChannelMember member : allMember) {
            if (member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN){
                uids.add(member.getUid());
            }
        }
    }
}
