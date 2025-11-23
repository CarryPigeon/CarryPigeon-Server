package team.carrypigeon.backend.chat.domain.controller.netty.user.register;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用户注册请求数据
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserRegisterVO implements CPControllerVO {
    private String email;
    private int code;

    @Override
    public boolean insertData(DefaultContext context) {
        if (email != null && code != 0){
            // 插入邮箱数据用于校验
            context.setData("Email",email);
            // 插入验证码数据
            context.setData("Email_Code",code);
            // 插入用户数据
            long id = IdUtil.generateId();
            context.setData("UserInfo_Id",id);
            context.setData("UserInfo_Email",email);
            context.setData("UserInfo_RegisterTime",TimeUtil.getCurrentTime());
            context.setData("UserInfo_Sex",CPUserSexEnum.UNKNOWN.getValue());
            context.setData("UserInfo_Brief","");
            context.setData("UserInfo_Birthday",TimeUtil.getCurrentTime());
            context.setData("UserInfo_Avatar",-1L);
            context.setData("UserInfo_Username",(id+"").substring(0,8));
            return true;
        }
        return false;
    }
}
