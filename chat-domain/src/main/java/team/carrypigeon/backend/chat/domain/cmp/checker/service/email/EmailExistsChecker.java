package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeBindKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

/**
 * 校验邮箱是否已被注册的节点。<br/>
 * 输入：Email:String<br/>
 * 输出：<br/>
 * <ul>
 *     <li>hard 模式：邮箱已存在时直接返回错误响应</li>
 *     <li>soft 模式（bind type=soft）：仅将结果写入 {@link CheckResult}</li>
 * </ul>
 * @author midreamsheep
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("EmailExistsChecker")
public class EmailExistsChecker extends CPNodeComponent {

    private static final String BIND_TYPE_KEY = CPNodeBindKeys.TYPE;

    private final team.carrypigeon.backend.api.dao.database.user.UserDao userDao;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        String type = getBindData(BIND_TYPE_KEY, String.class);
        boolean soft = "soft".equalsIgnoreCase(type);

        String email = context.getData(CPNodeValueKeyExtraConstants.EMAIL);
        if (email == null) {
            log.error("EmailExistsChecker args error: Email is null");
            argsError(context);
            return;
        }
        if (userDao.getByEmail(email) != null) {
            if (soft) {
                context.setData(CPNodeCommonKeys.CHECK_RESULT,
                        new CheckResult(false, "email exists"));
                log.info("EmailExistsChecker soft fail: email exists, email={}", email);
                return;
            }
            log.info("EmailExistsChecker hard fail: email exists, email={}", email);
            context.setData(CPNodeCommonKeys.RESPONSE,
                    CPResponse.ERROR_RESPONSE.copy().setTextData("email exists"));
            throw new CPReturnException();
        }
        if (soft) {
            context.setData(CPNodeCommonKeys.CHECK_RESULT, new CheckResult(true, null));
            log.debug("EmailExistsChecker soft success, email={}", email);
        }
    }
}
