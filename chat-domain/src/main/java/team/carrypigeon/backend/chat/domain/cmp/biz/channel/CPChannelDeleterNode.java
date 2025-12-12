package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.node.AbstractDeleteNode;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

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

    @Override
    protected String getContextKey() {
        return CPNodeChannelKeys.CHANNEL_INFO;
    }

    @Override
    protected Class<CPChannel> getEntityClass() {
        return CPChannel.class;
    }

    @Override
    protected boolean doDelete(CPChannel entity) {
        // 仅删除频道本身，成员删除延迟到 afterSuccess
        return channelDao.delete(entity);
    }

    @Override
    protected String getErrorMessage() {
        return "error deleting channel";
    }

    @Override
    protected void afterSuccess(CPChannel entity, DefaultContext context) throws CPReturnException {
        // 频道删除成功后，清理所有成员记录
        for (CPChannelMember channelMember : channelMemberDao.getAllMember(entity.getId())) {
            if (!channelMemberDao.delete(channelMember)) {
                businessError(context, "error deleting channel member");
            }
        }
    }
}
