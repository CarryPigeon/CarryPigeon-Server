package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * 用于从数据库获取通道数据的Node<br/>
 * 通过bind数据限制查询模式<br/>
 * 绑定数据：id<br/>
 * 入参：ChannelInfo_Id:Long<br/>
 * 出参: ChannelInfo:{@link CPChannel}<br/>
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelSelector")
public class CPChannelSelectorNode extends AbstractSelectorNode<CPChannel> {

    private final ChannelDao channelDao;

    @Override
    protected CPChannel doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode){
            case "id":
                Long id = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID, Long.class);
                return select(context,
                        buildSelectKey("channel", "id", id),
                        () -> channelDao.getById(id));
            default:
                argsError(context);
                return null;
        }
    }

    @Override
    protected String getResultKey() {
        return CPNodeChannelKeys.CHANNEL_INFO;
    }

    @Override
    protected void handleNotFound(String mode, CPFlowContext context) throws CPReturnException {
        // 通道不存在视为参数错误，保持与原实现一致
        argsError(context);
    }
}
