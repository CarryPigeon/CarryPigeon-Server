package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
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

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（当前支持 { id}）
     * @param context LiteFlow 上下文，读取频道标识并执行查询
     * @return 查询到的频道实体；未命中时返回 { null}
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected CPChannel doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode){
            case "id":
                Long id = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
                return select(context,
                        buildSelectKey("channel", "id", id),
                        () -> channelDao.getById(id));
            default:
                validationFailed();
                return null;
        }
    }

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 频道实体写入键 { CHANNEL_INFO}
     */
    @Override
    protected CPKey<CPChannel> getResultKey() {
        return CPNodeChannelKeys.CHANNEL_INFO;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式（当前支持 { id}）
     * @param context LiteFlow 上下文
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        fail(CPProblem.of(CPProblemReason.NOT_FOUND, "channel not found"));
    }
}
