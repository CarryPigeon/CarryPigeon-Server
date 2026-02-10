package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
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

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，读取用户信息并创建新的访问令牌
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        CPUser userInfo = requireContext(context, CPNodeUserKeys.USER_INFO);
        CPUserToken cpUserToken = new CPUserToken();
        cpUserToken
                .setId(IdUtil.generateId())
                .setUid(userInfo.getId())
                .setToken(IdUtil.generateToken())
                .setExpiredTime(TimeUtil.currentLocalDateTime().plusDays(30));
        if (!userTokenDao.save(cpUserToken)){
            fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "save user token error"));
        }
        context.set(CPNodeUserTokenKeys.USER_TOKEN_INFO, cpUserToken);
    }
}
