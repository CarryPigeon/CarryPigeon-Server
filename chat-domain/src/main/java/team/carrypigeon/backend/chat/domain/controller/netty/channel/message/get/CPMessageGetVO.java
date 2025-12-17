package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.get;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 获取单条频道消息的请求参数。<br/>
 * 入参：mid(消息id)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageGetVO implements CPControllerVO {

    /**
     * 消息 id
     */
    private long mid;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (mid <= 0) {
            return false;
        }
        context.setData(CPNodeMessageKeys.MESSAGE_INFO_ID, mid);
        return true;
    }
}
