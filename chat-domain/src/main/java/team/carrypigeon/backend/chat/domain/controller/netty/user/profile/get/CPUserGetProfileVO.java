package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 用户获取用户信息请求参数
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserGetProfileVO implements CPControllerVO {
    private long uid;
    @Override
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeUserKeys.USER_INFO_ID, uid);
        return true;
    }
}
