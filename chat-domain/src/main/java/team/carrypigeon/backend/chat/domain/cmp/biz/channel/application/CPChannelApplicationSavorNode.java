package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSaveNode;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

/**
 * 频道申请保存Node<br/>
 * 入参：<br/>
 * ChannelApplicationInfo:{@link CPChannelApplication}<br/>
 * 出参：无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationSavor")
public class CPChannelApplicationSavorNode extends AbstractSaveNode<CPChannelApplication> {

    private final ChannelApplicationDAO channelApplicationDAO;
    private final ApiWsEventPublisher wsEventPublisher;

    /**
     * 返回当前实体在上下文中的键。
     *
     * @return 申请实体在上下文中的键 { CHANNEL_APPLICATION_INFO}
     */
    @Override
    protected CPKey<CPChannelApplication> getContextKey() {
        return CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO;
    }

    /**
     * 返回当前节点处理的实体类型。
     *
     * @return 组件处理的实体类型 { CPChannelApplication}
     */
    @Override
    protected Class<CPChannelApplication> getEntityClass() {
        return CPChannelApplication.class;
    }

    /**
     * 执行保存操作并返回是否成功。
     *
     * @param entity 待保存的申请实体
     * @return {@code true} 表示申请写库成功
     */
    @Override
    protected boolean doSave(CPChannelApplication entity) {
        return channelApplicationDAO.save(entity);
    }

    /**
     * 返回失败分支使用的默认错误信息。
     *
     * @return 保存失败时返回给上层的默认错误描述
     */
    @Override
    protected String getErrorMessage() {
        return "save channel application error";
    }

    /**
     * 处理成功后的附加逻辑。
     *
     * @param entity 待保存的申请实体
     * @param context LiteFlow 上下文（当前用于链路一致性）
     */
    @Override
    protected void afterSuccess(CPChannelApplication entity, CPFlowContext context) {
        if (entity == null) {
            return;
        }
        wsEventPublisher.publishChannelChangedToChannelMembers(entity.getCid(), "applications");
    }
}
