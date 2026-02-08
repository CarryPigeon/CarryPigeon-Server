package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

import java.util.List;

/**
 * 删除频道成员的组件。<br/>
 * 不允许删除频道所有者和管理员。<br/>
 * 入参：ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参：无
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberDeleter")
public class CPChannelMemberDeleterNode extends AbstractDeleteNode<CPChannelMember> {

    private final ChannelMemberDao channelMemberDao;
    private final ChannelDao channelDao;
    private final ApiWsEventPublisher wsEventPublisher;

    @Override
    protected CPKey<CPChannelMember> getContextKey() {
        return CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO;
    }

    @Override
    protected Class<CPChannelMember> getEntityClass() {
        return CPChannelMember.class;
    }

    @Override
    protected boolean doDelete(CPChannelMember member) {
        if (isOwner(member, null)) {
            return false;
        }
        if (member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            return false;
        }
        return channelMemberDao.delete(member);
    }

    @Override
    protected String getErrorMessage() {
        return "error deleting channel member";
    }

    @Override
    protected void onFailure(CPChannelMember member, CPFlowContext context) {
        if (isOwner(member, context)) {
            log.info("CPChannelMemberDeleter refuse to delete owner, uid={}, cid={}",
                    member.getUid(), member.getCid());
            fail(CPProblem.of(403, "forbidden", "cannot delete channel owner"));
        }
        if (member != null && member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            log.info("CPChannelMemberDeleter refuse to delete admin, uid={}, cid={}",
                    member.getUid(), member.getCid());
            fail(CPProblem.of(403, "forbidden", "cannot delete channel admin"));
        } else if (member != null) {
            log.error("CPChannelMemberDeleter delete failed, uid={}, cid={}",
                    member.getUid(), member.getCid());
            fail(CPProblem.of(500, "internal_error", "error deleting channel member"));
        } else {
            fail(CPProblem.of(500, "internal_error", "error deleting channel member"));
        }
    }

    @Override
    protected void afterSuccess(CPChannelMember member, CPFlowContext context) {
        log.info("CPChannelMemberDeleter success, uid={}, cid={}", member.getUid(), member.getCid());
        wsEventPublisher.publishChannelChangedToChannelMembers(member.getCid(), "members");
        wsEventPublisher.publishChannelsChanged(List.of(member.getUid()));
    }

    private boolean isOwner(CPChannelMember member, CPFlowContext context) {
        if (member == null) {
            return false;
        }
        CPChannel channel = context == null ? null : context.get(CPNodeChannelKeys.CHANNEL_INFO);
        if (channel == null) {
            channel = channelDao.getById(member.getCid());
        }
        return channel != null && channel.getOwner() == member.getUid();
    }
}
