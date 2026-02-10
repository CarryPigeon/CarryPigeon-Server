package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

import java.util.List;

/**
 * 用于保存频道成员的Node<br/>
 * 入参: ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelMemberSaver")
public class CPChannelMemberSaverNode extends AbstractSaveNode<CPChannelMember> {

    private final ChannelMemberDao channelMemberDao;
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
     * 执行保存操作并返回是否成功。
     *
     * @param entity 待保存的成员实体
     * @return {@code true} 表示成员记录保存成功
     */
    @Override
    protected boolean doSave(CPChannelMember entity) {
        return channelMemberDao.save(entity);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 保存失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "save channel member error";
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param entity 待保存的成员实体
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void afterSuccess(CPChannelMember entity, CPFlowContext context) {
        if (entity == null) {
            return;
        }
        wsEventPublisher.publishChannelChangedToChannelMembers(entity.getCid(), "members");
        wsEventPublisher.publishChannelsChanged(List.of(entity.getUid()));
    }
}
