package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

/**
 * 用于保存通道信息的Node<br/>
 * 入参: ChannelInfo:{@link CPChannel}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelSaver")
public class CPChannelSaverNode extends AbstractSaveNode<CPChannel> {

    private final ChannelDao channelDao;
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
     * 执行保存操作并返回是否成功。
     *
     * @param entity 待保存的频道实体
     * @return {@code true} 表示频道数据保存成功
     */
    @Override
    protected boolean doSave(CPChannel entity) {
        return channelDao.save(entity);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 保存失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "save channel error";
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param entity 待保存的频道实体
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void afterSuccess(CPChannel entity, CPFlowContext context) {
        if (entity == null) {
            return;
        }
        wsEventPublisher.publishChannelChangedToChannelMembers(entity.getId(), "profile");
    }
}
