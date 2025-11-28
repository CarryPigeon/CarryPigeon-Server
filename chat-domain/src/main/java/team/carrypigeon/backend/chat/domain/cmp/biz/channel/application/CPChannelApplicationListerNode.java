package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.PageInfo;

import java.util.HashSet;

/**
 * 用于获取通道申请列表的Node<br/>
 * 入参: ChannelInfo_Id:Long;PageInfo:{@link PageInfo}<br/>
 * 出参：applications:Set<CPChannelApplication><br/>
 * @author midreamsheep
 */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationLister")
public class CPChannelApplicationListerNode extends CPNodeComponent {

    private final ChannelApplicationDAO channelApplicationDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        Long channelId = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID);
        PageInfo pageInfo = context.getData(CPNodeValueKeyExtraConstants.PAGE_INFO);
        if (channelId == null || pageInfo == null){
            argsError(context);
            return;
        }
        CPChannelApplication[] applications = channelApplicationDao.getByCid(channelId, pageInfo.page(), pageInfo.pageSize());
        HashSet<CPChannelApplication> objects = new HashSet<>();
        for (CPChannelApplication application : applications) {
            objects.add(application);
        }
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO_LIST, objects);
    }
}
