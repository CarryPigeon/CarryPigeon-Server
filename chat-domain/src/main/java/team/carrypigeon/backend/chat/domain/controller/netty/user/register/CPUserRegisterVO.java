package team.carrypigeon.backend.chat.domain.controller.netty.user.register;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用户注册请求数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserRegisterVO implements CPControllerVO {
    private String email;
    private int code;

    @Override
    public boolean insertData(DefaultContext context) {
        if (email != null && !email.isEmpty() && code != 0) {
            // 插入邮箱数据用于后续校验
            context.setData(CPNodeValueKeyExtraConstants.EMAIL, email);
            // 插入验证码数据
            context.setData(CPNodeValueKeyExtraConstants.EMAIL_CODE, code);
            // 插入用户基础数据
            long id = IdUtil.generateId();
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_ID, id);
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_EMAIL, email);
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_REGISTER_TIME, TimeUtil.getCurrentTime());
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_SEX, CPUserSexEnum.UNKNOWN.getValue());
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BRIEF, "");
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_BIRTHDAY, TimeUtil.getCurrentTime());
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_AVATAR, -1L);
            context.setData(CPNodeValueKeyBasicConstants.USER_INFO_USER_NAME, (id + "").substring(0, 8));
            return true;
        }
        return false;
    }
}
