package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 校验邮箱是否已被注册的节点。<br/>
 * 输入：Email:String<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：邮箱已存在时直接返回错误响应</li>
 *     <li>soft 模式（bind type=soft）：仅将结果写入 {@link team.carrypigeon.backend.api.chat.domain.flow.CheckResult}</li>
 * </ul>
 * @author midreamsheep
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("EmailExistsChecker")
public class EmailExistsChecker extends AbstractCheckerNode {

    private final team.carrypigeon.backend.api.dao.database.user.UserDao userDao;

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前请求会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取邮箱并校验是否已注册
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        boolean soft = isSoftMode();
        String email = requireContext(context, CPNodeValueKeyExtraConstants.EMAIL);
        if (select(context,
                buildSelectKey("user", "email", email),
                () -> userDao.getByEmail(email)) != null) {
            if (soft) {
                markSoftFail(context, "email exists");
                log.info("EmailExistsChecker soft fail: email exists, email={}", email);
                return;
            }
            log.info("EmailExistsChecker hard fail: email exists, email={}", email);
            fail(CPProblem.of(CPProblemReason.EMAIL_EXISTS, "email exists"));
        }
        if (soft) {
            markSoftSuccess(context);
            log.debug("EmailExistsChecker soft success, email={}", email);
        }
    }
}
