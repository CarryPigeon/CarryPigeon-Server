package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.cmp.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.cmp.CPReturnException;

/**
 * 保存用户token的Node<br/>
 * 入参: UserToken:{@link CPUserToken}<br/>
 * 出参: 无<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenSaver")
public class CPUserTokenSaverNode extends CPNodeComponent {

    private final UserTokenDao userTokenDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        CPUserToken userToken = context.getData("UserToken");
        if (!userTokenDao.save(userToken)){
            context.setData("response", CPResponse.ERROR_RESPONSE.copy().setTextData("save user token error"));
            throw new CPReturnException();
        }
        userTokenDao.save(userToken);
    }
}
