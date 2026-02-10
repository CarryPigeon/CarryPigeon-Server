package team.carrypigeon.backend.chat.domain.cmp.biz.user.token;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserTokenKeys;

/**
 * 用于从数据库查询token<br/>
 * bind绑定查询格式key:{token|id}<br/>
 * 入参: UserToken_Token:{@link String}|id:{@link Long}<br/>
 * 出参: UserToken:{@link CPUserToken}
 * @author midreamsheep
 * */
@AllArgsConstructor
@LiteflowComponent("CPUserTokenSelector")
public class CPUserTokenSelectorNode extends AbstractSelectorNode<CPUserToken> {
    private final UserTokenDao userTokenDao;

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（支持 { token} 与 { id}）
     * @param context LiteFlow 上下文，读取令牌查询条件
     * @return 命中的令牌实体；未命中时返回 { null}
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected CPUserToken doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode){
            case "token":
                String token =
                        requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO_TOKEN);
                return select(context,
                        buildSelectKey("user_token", "token", token),
                        () -> userTokenDao.getByToken(token));
            case "id":
                Long id = requireContext(context, CPNodeUserTokenKeys.USER_TOKEN_INFO_ID);
                return select(context,
                        buildSelectKey("user_token", "id", id),
                        () -> userTokenDao.getById(id));
            default:
                validationFailed();
                return null;
        }
    }

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 令牌实体写入键 { USER_TOKEN_INFO}
     */
    @Override
    protected CPKey<CPUserToken> getResultKey() {
        return CPNodeUserTokenKeys.USER_TOKEN_INFO;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式（支持 { token} 与 { id}）
     * @param context LiteFlow 上下文
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        fail(CPProblem.of(CPProblemReason.NOT_FOUND, "token does not exists"));
    }
}
