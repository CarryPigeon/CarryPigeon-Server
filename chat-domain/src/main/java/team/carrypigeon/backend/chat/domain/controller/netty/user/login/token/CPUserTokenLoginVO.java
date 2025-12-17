package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

/**
 * 用户通过token登录的参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserTokenLoginVO implements CPControllerVO {
    // 用户登录token
    private String token;

    @Override
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, token);
        return true;
    }
}
