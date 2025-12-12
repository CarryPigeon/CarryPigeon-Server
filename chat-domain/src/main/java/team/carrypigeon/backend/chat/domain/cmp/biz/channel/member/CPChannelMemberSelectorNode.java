package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

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
public class CPChannelMemberSelectorNode extends AbstractSelectorNode<CPChannelMember> {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected CPChannelMember doSelect(String mode, DefaultContext context) throws Exception {
        switch (mode){
            case "id":
                Long channelMemberInfoId =
                        requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_ID, Long.class);
                return channelMemberDao.getById(channelMemberInfoId);
            case "CidWithUid":
                Long cid =
                        requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID, Long.class);
                Long uid =
                        requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, Long.class);
                return channelMemberDao.getMember(uid, cid);
            default:
                argsError(context);
                return null;
        }
    }

    @Override
    protected String getResultKey() {
        return CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO;
    }

    @Override
    protected void handleNotFound(String mode, DefaultContext context) throws CPReturnException {
        businessError(context, "channel member not found");
    }
}
