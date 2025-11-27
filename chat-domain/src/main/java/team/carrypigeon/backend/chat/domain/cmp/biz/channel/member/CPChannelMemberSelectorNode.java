package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;

/**
 * 用于选择通道成员的Node<br/>
 * bind:String(CidWithUid|id)<br/>
 * 入参: <br/>
 * 1. CidWithUid->ChannelMemberInfo_Cid:Long;ChannelMemberInfo_Uid:Long<br/>
 * 2. id->ChannelMemberInfo_Id:Long<br/>
 * 出参: ChannelMemberInfo:{@link CPChannelMember}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberSelector")
public class CPChannelMemberSelectorNode extends CPNodeComponent {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String bindData = getBindData("key", String.class);
        if (bindData == null){
            argsError(context);
        }
        switch (bindData){
            case "id":
                Long channelMemberInfoId = context.getData("ChannelMemberInfo_Id");
                if (channelMemberInfoId == null){
                    argsError(context);
                }
                CPChannelMember channelMemberInfo = channelMemberDao.getById(channelMemberInfoId);
                if (channelMemberInfo == null){
                    context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("channel member not found"));
                    throw new CPReturnException();
                }
                context.setData("ChannelMemberInfo", channelMemberInfo);
                break;
            case "CidWithUid":
                Long cid = context.getData("ChannelMemberInfo_Cid");
                Long uid = context.getData("ChannelMemberInfo_Uid");
                if (cid == null || uid == null) {
                    argsError(context);
                }
                CPChannelMember channelMemberInfo2 = channelMemberDao.getMember(uid, cid);
                if (channelMemberInfo2 == null){
                    context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("channel member not found"));
                    throw new CPReturnException();
                }
                context.setData("ChannelMemberInfo", channelMemberInfo2);
                break;
            case null:
            default:
                argsError(context);
                break;
        }
    }
}
