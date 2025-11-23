package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;

/**
 * 获取用户token的uid<br/>
 * 入参: UserToken:{@link CPUserToken}<br/>
 * 出参: UserInfo_Id:{@link Long}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenUidGetter")
public class CPUserTokenUidGetterNode extends CPNodeComponent {
    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUserToken userToken = context.getData("UserToken");
        if (userToken == null){
            argsError(context);
        }
        context.setData("UserInfo_Id",userToken.getUid());
    }
}
