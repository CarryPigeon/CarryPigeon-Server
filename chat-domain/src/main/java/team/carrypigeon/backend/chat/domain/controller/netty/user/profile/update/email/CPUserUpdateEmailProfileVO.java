package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update.email;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserUpdateEmailProfileVO implements CPControllerVO {
    private String newEmail;
    private int code;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData("Email", newEmail);
        context.setData("Email_Code", code);
        context.setData("UserInfo_Email", newEmail);
        return true;
    }
}
