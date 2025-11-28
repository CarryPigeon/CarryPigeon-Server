package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 用于保存通道信息的Node<br/>
 * 入参: ChannelInfo:{@link CPChannel}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelSaver")
public class CPChannelSaverNode extends CPNodeComponent {

    private final ChannelDao channelDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPChannel channelInfo = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO);
        if (channelInfo == null){
            argsError(context);
        }
        if (!channelDao.save(channelInfo)){
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("save channel error"));
            throw new CPReturnException();
        }
    }
}
