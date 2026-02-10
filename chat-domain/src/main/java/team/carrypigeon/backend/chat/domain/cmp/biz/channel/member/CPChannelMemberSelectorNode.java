package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
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

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（支持 { id} 与 { CidWithUid}）
     * @param context LiteFlow 上下文，读取成员查询条件
     * @return 命中的成员实体；未命中时返回 { null}
     * @throws Exception 执行过程中抛出的异常
     */
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

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 成员实体写入键 { CHANNEL_MEMBER_INFO}
     */
    @Override
    protected CPKey<CPChannelMember> getResultKey() {
        return CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式（支持 { id} 与 { CidWithUid}）
     * @param context LiteFlow 上下文
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        fail(CPProblem.of(CPProblemReason.NOT_CHANNEL_MEMBER, "not a channel member"));
    }
}
