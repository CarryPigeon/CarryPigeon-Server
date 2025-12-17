package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

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
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeUserKeys.USER_INFO_USER_NAME, username);
        context.setData(CPNodeUserKeys.USER_INFO_AVATAR, avatar);
        context.setData(CPNodeUserKeys.USER_INFO_SEX, sex);
        context.setData(CPNodeUserKeys.USER_INFO_BRIEF, brief);
        context.setData(CPNodeUserKeys.USER_INFO_BIRTHDAY, birthday);
        return true;
    }
}
