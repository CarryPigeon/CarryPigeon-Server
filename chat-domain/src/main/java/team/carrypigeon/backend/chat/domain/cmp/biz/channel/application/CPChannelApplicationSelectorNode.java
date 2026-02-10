package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;

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

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（当前支持 { id}）
     * @param context LiteFlow 上下文，读取申请标识并执行查询
     * @return 查询到的申请实体；未命中时返回 { null}
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected CPChannelApplication doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode) {
            case "id":
                Long applicationId =
                        requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_ID);
                return select(context,
                        buildSelectKey("channel_application", "id", applicationId),
                        () -> channelApplicationDao.getById(applicationId));
            default:
                validationFailed();
                return null;
        }
    }

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 申请实体写入键 { CHANNEL_APPLICATION_INFO}
     */
    @Override
    protected CPKey<CPChannelApplication> getResultKey() {
        return CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式（当前支持 { id}）
     * @param context LiteFlow 上下文
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        validationFailed();
    }
}
