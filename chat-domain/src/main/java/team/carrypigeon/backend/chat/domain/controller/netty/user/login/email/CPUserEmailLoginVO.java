package team.carrypigeon.backend.chat.domain.controller.netty.user.login.email;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 用户邮箱登录请求数据
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserEmailLoginVO implements CPControllerVO {
    // 用户邮箱
    private String email;
    // 验证码
    private int code;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, email);
        context.setData(CPNodeValueKeyExtraConstants.EMAIL_CODE, code);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_EMAIL, email);
        return true;
    }
}
