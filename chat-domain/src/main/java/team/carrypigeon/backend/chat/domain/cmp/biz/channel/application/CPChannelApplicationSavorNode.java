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

    @Override
    protected CPKey<CPChannelApplication> getContextKey() {
        return CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO;
    }

    @Override
    protected Class<CPChannelApplication> getEntityClass() {
        return CPChannelApplication.class;
    }

    @Override
    protected boolean doSave(CPChannelApplication entity) {
        return channelApplicationDAO.save(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "save channel application error";
    }

    @Override
    protected void afterSuccess(CPChannelApplication entity, CPFlowContext context) {
        if (entity == null) {
            return;
        }
        wsEventPublisher.publishChannelChangedToChannelMembers(entity.getCid(), "applications");
    }
}
