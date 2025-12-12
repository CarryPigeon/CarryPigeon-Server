package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用户更新token节点<br/>
 * 入参:<br/>
 * 1. UserToken:{@link CPUserToken}<br/>
 * 2. UserToken_Token:String? <br/>
 * 3. UserToken_ExpiredTime:Long? <br/>
 * 出参：无
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenUpdater")
public class CPUserTokenUpdaterNode extends CPNodeComponent {

    private final UserTokenDao userTokenDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        CPUserToken userToken = context.getData(CPNodeUserTokenKeys.USER_TOKEN_INFO);
        if (userToken == null){
            argsError(context);
        }
        String token = context.getData(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN);
        if (token != null){
            userToken.setToken(token);
        }
        Long expiredTime = context.getData(CPNodeUserTokenKeys.USER_TOKEN_INFO_EXPIRED_TIME);
        if (expiredTime != null){
            userToken.setExpiredTime(TimeUtil.MillisToLocalDateTime(expiredTime));
        }
        if (!userTokenDao.save(userToken)){
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("update user token error"));
            throw new CPReturnException();
        }

    }
}
