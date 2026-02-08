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

    @Override
    protected CPKey<CPChannel> getContextKey() {
        return CPNodeChannelKeys.CHANNEL_INFO;
    }

    @Override
    protected Class<CPChannel> getEntityClass() {
        return CPChannel.class;
    }

    @Override
    protected boolean doSave(CPChannel entity) {
        return channelDao.save(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "save channel error";
    }

    @Override
    protected void afterSuccess(CPChannel entity, CPFlowContext context) {
        if (entity == null) {
            return;
        }
        // Profile changes impact channel view.
        wsEventPublisher.publishChannelChangedToChannelMembers(entity.getId(), "profile");
    }
}
