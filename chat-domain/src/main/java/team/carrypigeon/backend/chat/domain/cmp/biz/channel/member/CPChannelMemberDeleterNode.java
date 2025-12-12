package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 删除频道成员的组件。<br/>
 * 不允许删除频道管理员。<br/>
 * 入参：ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参：无
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberDeleter")
public class CPChannelMemberDeleterNode extends AbstractDeleteNode<CPChannelMember> {

    private final ChannelMemberDao channelMemberDao;

    @Override
    protected String getContextKey() {
        return CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO;
    }

    @Override
    protected Class<CPChannelMember> getEntityClass() {
        return CPChannelMember.class;
    }

    @Override
    protected boolean doDelete(CPChannelMember member) {
        // 管理员不允许删除，视为删除失败，交给 onFailure 处理
        if (member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            return false;
        }
        return channelMemberDao.delete(member);
    }

    @Override
    protected String getErrorMessage() {
        // 默认错误信息，不适用于管理员场景，由 onFailure 自行处理
        return "error deleting channel member";
    }

    @Override
    protected void onFailure(CPChannelMember member, DefaultContext context) throws CPReturnException {
        if (member != null && member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            log.info("CPChannelMemberDeleter refuse to delete admin, uid={}, cid={}",
                    member.getUid(), member.getCid());
            businessError(context, "cannot delete channel admin");
        } else if (member != null) {
            log.error("CPChannelMemberDeleter delete failed, uid={}, cid={}",
                    member.getUid(), member.getCid());
            businessError(context, "error deleting channel member");
        } else {
            businessError(context, "error deleting channel member");
        }
    }

    @Override
    protected void afterSuccess(CPChannelMember member, DefaultContext context) {
        log.info("CPChannelMemberDeleter success, uid={}, cid={}", member.getUid(), member.getCid());
    }
}
