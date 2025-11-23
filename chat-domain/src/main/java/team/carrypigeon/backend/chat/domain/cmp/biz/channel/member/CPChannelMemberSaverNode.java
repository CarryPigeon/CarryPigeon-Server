package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.CPReturnException;

/**
 * 用于保存频道成员的Node<br/>
 * 入参: ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberSaver")
public class CPChannelMemberSaverNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannelMember channelMemberInfo = context.getData("ChannelMemberInfo");
        if (channelMemberInfo == null){
            argsError(context);
        }
        if (!channelMemberDao.save(channelMemberInfo)){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("save channel member error"));
            throw new CPReturnException();
        }
    }
}
