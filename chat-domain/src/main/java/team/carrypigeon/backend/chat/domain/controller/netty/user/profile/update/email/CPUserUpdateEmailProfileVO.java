package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update.email;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserUpdateEmailProfileVO implements CPControllerVO {
    private String newEmail;
    private int code;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, newEmail);
        context.setData(CPNodeValueKeyExtraConstants.EMAIL_CODE, code);
        context.setData(CPNodeUserKeys.USER_INFO_EMAIL, newEmail);
        return true;
    }
}
