package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;
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
public class CPUserTokenCreatorNode extends CPNodeComponent {

    private final UserTokenDao userTokenDao;

    @Override
    protected void process(CPFlowContext context) throws Exception {
        // 查询数据
        CPUser userInfo = requireContext(context, CPNodeUserKeys.USER_INFO);
        // 创建token
        CPUserToken cpUserToken = new CPUserToken();
        cpUserToken
                .setId(IdUtil.generateId())
                .setUid(userInfo.getId())
                .setToken(IdUtil.generateToken())
                .setExpiredTime(TimeUtil.currentLocalDateTime().plusDays(30));
        if (!userTokenDao.save(cpUserToken)){
            fail(CPProblem.of(500, "internal_error", "save user token error"));
        }
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO, cpUserToken);
    }
}
