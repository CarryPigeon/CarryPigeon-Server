package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
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

    /**
     * 返回当前实体在上下文中的键。
     *
     * @return 成员实体在上下文中的键 { CHANNEL_MEMBER_INFO}
     */
    @Override
    protected CPKey<CPChannelMember> getContextKey() {
        return CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO;
    }

    /**
     * 返回当前节点处理的实体类型。
     *
     * @return 组件处理的实体类型 { CPChannelMember}
     */
    @Override
    protected Class<CPChannelMember> getEntityClass() {
        return CPChannelMember.class;
    }

    /**
     * 执行删除操作并返回是否成功。
     *
     * @param member 待删除的成员实体
     * @return {@code true} 表示成员记录删除成功
     */
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

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 删除失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "error deleting channel member";
    }

    /**
     * 处理失败后的补偿逻辑。
     *
     * @param member 删除失败的成员实体（可能为 {@code null}）
     * @param context LiteFlow 上下文，用于判定失败原因并中断链路
     */
    @Override
    protected void onFailure(CPChannelMember member, CPFlowContext context) {
        if (isOwner(member, context)) {
            log.info("CPChannelMemberDeleter refuse to delete owner, uid={}, cid={}",
                    member.getUid(), member.getCid());
            fail(CPProblem.of(CPProblemReason.FORBIDDEN, "cannot delete channel owner"));
        }
        if (member != null && member.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            log.info("CPChannelMemberDeleter refuse to delete admin, uid={}, cid={}",
                    member.getUid(), member.getCid());
            fail(CPProblem.of(CPProblemReason.FORBIDDEN, "cannot delete channel admin"));
        } else if (member != null) {
            log.error("CPChannelMemberDeleter delete failed, uid={}, cid={}",
                    member.getUid(), member.getCid());
            fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "error deleting channel member"));
        } else {
            fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "error deleting channel member"));
        }
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param member 已删除成功的成员实体
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void afterSuccess(CPChannelMember member, CPFlowContext context) {
        log.info("CPChannelMemberDeleter success, uid={}, cid={}", member.getUid(), member.getCid());
        wsEventPublisher.publishChannelChangedToChannelMembers(member.getCid(), "members");
        wsEventPublisher.publishChannelsChanged(List.of(member.getUid()));
    }

    /**
     * 判断待删除成员是否为频道拥有者。
     *
     * @param member 待校验的成员实体
     * @param context LiteFlow 上下文（为空时将回退数据库查询）
     * @return {@code true} 表示该成员为频道拥有者
     */
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
