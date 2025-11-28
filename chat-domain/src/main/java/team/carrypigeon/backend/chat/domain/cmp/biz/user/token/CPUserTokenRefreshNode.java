package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用户token刷新节点<br/>
 * 入参: UserToken:{@link CPUserToken}<br/>
 * 出参: UserToken:{@link CPUserToken}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenRefresh")
public class CPUserTokenRefreshNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUserToken userToken = context.getData(CPNodeValueKeyExtraConstants.USER_TOKEN);
        if (userToken == null){
            argsError(context);
        }
        userToken.setExpiredTime(TimeUtil.getCurrentLocalTime().plusDays(30));
        userToken.setToken(IdUtil.generateToken());
    }
}
