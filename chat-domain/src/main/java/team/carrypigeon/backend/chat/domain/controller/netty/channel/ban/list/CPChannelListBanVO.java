package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * 获取频道封禁列表的请求参数。
 * <p>
 * 只包含一个字段：频道 id。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelListBanVO implements CPControllerVO {

    /**
     * 频道 id
     */
    private long cid;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (cid <= 0) {
            return false;
        }
        // 写入频道标识，供后续节点查询封禁记录
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        return true;
    }
}
