package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 选择通道申请Node<br/>
 * bind:String(id)
 * 入参：CPChannelApplicationInfo_Id:Long<br/>
 * 出参：CPChannelApplicationInfo:{@link CPChannelApplication}
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPChannelApplicationSelector")
public class CPChannelApplicationSelectorNode extends AbstractSelectorNode<CPChannelApplication> {

    private final ChannelApplicationDAO channelApplicationDao;

    @Override
    protected CPChannelApplication doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode) {
            case "id":
                Long applicationId =
                        requireContext(context, CPNodeValueKeyExtraConstants.CHANNEL_APPLICATION_INFO_ID, Long.class);
                return select(context,
                        buildSelectKey("channel_application", "id", applicationId),
                        () -> channelApplicationDao.getById(applicationId));
            default:
                argsError(context);
                return null;
        }
    }

    @Override
    protected String getResultKey() {
        return CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO;
    }

    @Override
    protected void handleNotFound(String mode, CPFlowContext context) throws CPReturnException {
        // 与原实现保持一致：视为参数错误
        argsError(context);
    }
}
