package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 更新用户信息请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserUpdateProfileVO implements CPControllerVO {
    private String username;
    private long avatar;
    private int sex;
    private String brief;
    private long birthday;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData("UserInfo_UserName", username);
        context.setData("UserInfo_Avatar", avatar);
        context.setData("UserInfo_Sex", sex);
        context.setData("UserInfo_Brief", brief);
        context.setData("UserInfo_Birthday", birthday);
        return true;
    }
}
