package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

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
    public boolean insertData(DefaultContext context) {
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN_TOKEN, token);
        return true;
    }
}
