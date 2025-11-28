package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用于生成用户token的Node<br/>
 * 入参: UserInfo:{@link CPUser}<br/>
 * 出参: UserToken:{@link CPUserToken}<br/>
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenCreator")
public class CPUUserTokenCreatorNode extends CPNodeComponent {

    private final UserTokenDao userTokenDao;

    @Override
    protected void process(CPSession session, DefaultContext context) throws Exception {
        // 查询数据
        CPUser userInfo = context.getData(CPNodeValueKeyBasicConstants.USER_INFO);
        if (userInfo == null){
            argsError(context);
        }
        // 创建token
        CPUserToken cpUserToken = new CPUserToken();
        cpUserToken
                .setId(IdUtil.generateId())
                .setUid(userInfo.getId())
                .setToken(IdUtil.generateToken())
                .setExpiredTime(TimeUtil.getCurrentLocalTime().plusDays(30));
        if (!userTokenDao.save(cpUserToken)){
            context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("save user token error"));
            throw new CPReturnException();
        }
        context.setData(CPNodeValueKeyExtraConstants.USER_TOKEN, cpUserToken);
    }
}
