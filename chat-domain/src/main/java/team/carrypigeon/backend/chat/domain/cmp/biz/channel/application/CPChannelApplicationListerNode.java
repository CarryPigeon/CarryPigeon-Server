package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
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
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long channelId = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID, Long.class);
        PageInfo pageInfo = requireContext(context, CPNodeValueKeyExtraConstants.PAGE_INFO, PageInfo.class);
        CPChannelApplication[] applications = channelApplicationDao.getByCid(channelId, pageInfo.page(), pageInfo.pageSize());
        HashSet<CPChannelApplication> objects = new HashSet<>();
        for (CPChannelApplication application : applications) {
            objects.add(application);
        }
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_LIST, objects);
    }
}
