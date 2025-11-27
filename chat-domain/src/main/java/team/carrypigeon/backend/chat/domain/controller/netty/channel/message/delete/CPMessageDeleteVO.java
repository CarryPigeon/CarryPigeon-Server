package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.delete;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 删除消息的请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageDeleteVO implements CPControllerVO {

    private long mid;

    @Override
    public boolean insertData(DefaultContext context) {
        if (mid <= 0) {
            return false;
        }
        context.setData("MessageInfo_Id", mid);
        return true;
    }
}
