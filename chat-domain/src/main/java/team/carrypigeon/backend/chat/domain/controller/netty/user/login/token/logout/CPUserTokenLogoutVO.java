package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token.logout;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserTokenLogoutVO implements CPControllerVO {
    private String token;
    @Override
    public boolean insertData(DefaultContext context) {
        context.setData(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN, token);
        return true;
    }
}
