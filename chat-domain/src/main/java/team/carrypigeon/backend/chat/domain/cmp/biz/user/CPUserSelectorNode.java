package team.carrypigeon.backend.chat.domain.cmp.biz.user;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractSelectorNode;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

/**
 * 根据用户 id 或邮箱从数据库中查询用户信息的 Selector 节点。<br/>
 * 查询模式通过 bind 参数 key 指定："id" / "email"。<br/>
 * 1. id 查询入参：UserInfo_Id:Long<br/>
 * 2. email 查询入参：UserInfo_Email:String<br/>
 * 出参：UserInfo:{@link CPUser}<br/>
 */
@AllArgsConstructor
@LiteflowComponent("CPUserSelector")
public class CPUserSelectorNode extends AbstractSelectorNode<CPUser> {

    private final UserDao userDao;

    /**
     * 按模式执行数据查询。
     *
     * @param mode 查询模式（支持 { id} 与 { email}）
     * @param context LiteFlow 上下文，读取用户查询条件
     * @return 命中的用户实体；未命中时返回 { null}
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected CPUser doSelect(String mode, CPFlowContext context) throws Exception {
        switch (mode) {
            case "id":
                Long id = requireContext(context, CPNodeUserKeys.USER_INFO_ID);
                return select(context,
                        buildSelectKey("user", "id", id),
                        () -> userDao.getById(id));
            case "email":
                String email = requireContext(context, CPNodeUserKeys.USER_INFO_EMAIL);
                return select(context,
                        buildSelectKey("user", "email", email),
                        () -> userDao.getByEmail(email));
            default:
                validationFailed();
                return null;
        }
    }

    /**
     * 返回查询结果写入的上下文键。
     *
     * @return 用户实体写入键 { USER_INFO}
     */
    @Override
    protected CPKey<CPUser> getResultKey() {
        return CPNodeUserKeys.USER_INFO;
    }

    /**
     * 处理未找到资源时的分支行为。
     *
     * @param mode 查询模式（支持 { id} 与 { email}）
     * @param context LiteFlow 上下文
     */
    @Override
    protected void handleNotFound(String mode, CPFlowContext context) {
        fail(CPProblem.of(CPProblemReason.NOT_FOUND, "user not found"));
    }
}
