package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 用户获取用户信息请求参数
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserGetProfileVO implements CPControllerVO {
    private long uid;
    @Override
    public boolean insertData(DefaultContext context) {
        context.setData("UserInfo_Id",uid);
        return true;
    }
}
