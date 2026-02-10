package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

/**
 * 用于删除通道的Node<br/>
 * 入参: ChannelInfo:{@link CPChannel}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelDeleter")
public class CPChannelDeleterNode extends AbstractDeleteNode<CPChannel> {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;
    private final ApiWsEventPublisher wsEventPublisher;

    /**
     * 返回当前实体在上下文中的键。
     *
     * @return 频道实体在上下文中的键 { CHANNEL_INFO}
     */
    @Override
    protected CPKey<CPChannel> getContextKey() {
        return CPNodeChannelKeys.CHANNEL_INFO;
    }

    /**
     * 返回当前节点处理的实体类型。
     *
     * @return 组件处理的实体类型 { CPChannel}
     */
    @Override
    protected Class<CPChannel> getEntityClass() {
        return CPChannel.class;
    }

    /**
     * 执行删除操作并返回是否成功。
     *
     * @param entity 待删除的频道实体
     * @return {@code true} 表示频道主记录删除成功
     */
    @Override
    protected boolean doDelete(CPChannel entity) {
        return channelDao.delete(entity);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 删除失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "error deleting channel";
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param entity 待删除的频道实体
     * @param context LiteFlow 上下文，用于触发后续成员清理与通知
     */
    @Override
    protected void afterSuccess(CPChannel entity, CPFlowContext context) {
        wsEventPublisher.publishChannelsChangedToChannelMembers(entity.getId());
        for (CPChannelMember channelMember : channelMemberDao.getAllMember(entity.getId())) {
            if (!channelMemberDao.delete(channelMember)) {
                fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "error deleting channel member"));
            }
        }
    }
}
