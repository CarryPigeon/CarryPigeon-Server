package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
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
    protected CPChannelMember doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode){
            case "id":
                Long channelMemberInfoId =
                        requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_ID);
                return select(context,
                        buildSelectKey("channel_member", "id", channelMemberInfoId),
                        () -> channelMemberDao.getById(channelMemberInfoId));
            case "CidWithUid":
                Long cid =
                        requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID);
                Long uid =
                        requireContext(context, CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID);
                return select(context,
                        buildSelectKey("channel_member", java.util.Map.of("cid", cid, "uid", uid)),
                        () -> channelMemberDao.getMember(uid, cid));
            default:
                validationFailed();
                return null;
        }
    }

    @Override
    protected CPKey<CPChannelMember> getResultKey() {
        return CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO;
    }

    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        // Used by /api chains (MemberGuard / ReadGuard): treat as forbidden.
        fail(CPProblem.of(403, "not_channel_member", "not a channel member"));
    }
}
