package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 创建消息的请求参数<br/>
 * type 为消息类型标识（例如 Core:Text），cid 为频道 id，data 为具体消息体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageCreateVO implements CPControllerVO {

    private String type;
    private long cid;
    private JsonNode data;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (type == null || type.isEmpty() || cid <= 0 || data == null) {
            return false;
        }
        // 频道基本信息
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        // 消息原始数据与域，后续由 CPMessageParserService 解析
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, type);
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_DATA, data);
        return true;
    }
}
