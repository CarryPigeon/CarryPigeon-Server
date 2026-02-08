package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
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
    protected void process(CPFlowContext context) throws Exception {
        CPUserToken userToken = requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO);
        String token = context.get(CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN);
        if (token != null){
            userToken.setToken(token);
        }
        Long expiredTime = context.get(CPNodeUserTokenKeys.USER_TOKEN_INFO_EXPIRED_TIME);
        if (expiredTime != null){
            userToken.setExpiredTime(TimeUtil.millisToLocalDateTime(expiredTime));
        }
        if (!userTokenDao.save(userToken)){
            fail(CPProblem.of(500, "internal_error", "update user token error"));
        }

    }
}
