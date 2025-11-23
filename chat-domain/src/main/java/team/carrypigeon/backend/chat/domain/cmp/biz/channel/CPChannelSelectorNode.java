package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;

/**
 * 用于从数据库获取通道数据的Node<br/>
 * 通过bind数据限制查询模式<br/>
 * 绑定数据：id<br/>
 * 入参：ChannelInfo_Id:Long<br/>
 * 出参: ChannelInfo:{@link CPChannel}<br/>
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelSelector")
public class CPChannelSelectorNode extends CPNodeComponent {

    private final ChannelDao channelDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String bindData = getBindData("key", String.class);
        switch (bindData){
            case "id":
                Long id = context.getData("ChannelInfo_Id");
                if (id == null){
                    argsError(context);
                }
                CPChannel channel = channelDao.getById(id);
                if (channel == null){
                    argsError(context);
                }
                context.setData("ChannelInfo",channel);
                break;
            case null:
            default:
                argsError(context);
        }
    }
}
