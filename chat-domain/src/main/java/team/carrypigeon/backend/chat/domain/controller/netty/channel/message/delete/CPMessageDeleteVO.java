package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.delete;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 删除消息的请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageDeleteVO implements CPControllerVO {

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
