package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;

/**
 * 选择通道申请Node<br/>
 * bind:String(id)
 * 入参：CPChannelApplicationInfo_Id:Long<br/>
 * 出参：CPChannelApplicationInfo:{@link CPChannelApplication}
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationSelector")
public class CPChannelApplicationSelectorNode extends CPNodeComponent {

    private final ChannelApplicationDAO channelApplicationDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        String bindData = getBindData("key", String.class);
        if (bindData == null){
            argsError(context);
            return;
        }
        switch (bindData){
            case "id":
                CPChannelApplication application = channelApplicationDao.getById(getBindData("id", Long.class));
                if (application == null){
                    argsError(context);
                    return;
                }
                context.setData("ChannelApplicationInfo",application);
                break;
            default:
                argsError( context);
                break;
        }
    }
}
