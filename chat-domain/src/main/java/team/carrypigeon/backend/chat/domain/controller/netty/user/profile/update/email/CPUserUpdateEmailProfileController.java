package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;

/**
 * 更新用户邮箱信息<br/>
 * 请求url: /core/user/profile/update/email<br/>
 * 请求参数: {@link CPUserUpdateEmailProfileVO}<br/>
 * */
@CPControllerTag("/core/user/profile/update/email")
public class CPUserUpdateEmailProfileController implements CPController {

    private final ObjectMapper objectMapper;
    private final CPCache cache;
    private final UserDao userDao;

    public CPUserUpdateEmailProfileController(ObjectMapper objectMapper, CPCache cache, UserDao userDao) {
        this.objectMapper = objectMapper;
        this.cache = cache;
        this.userDao = userDao;
    }

    @Override
    @LoginPermission
    public CPResponse process(CPSession session, JsonNode data) {
        // 数据解析
        CPUserUpdateEmailProfileVO cpUserUpdateEmailProfileVO;
        try {
            cpUserUpdateEmailProfileVO = objectMapper.treeToValue(data, CPUserUpdateEmailProfileVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 判断数据有效性
        if (!cpUserUpdateEmailProfileVO.getNewEmail().matches("^[a-zA-Z0-9_+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("email error");
        }

        // 通过缓存判断是否验证码正确
        String code = cache.getAndDelete(cpUserUpdateEmailProfileVO.getNewEmail() + ":code");
        if (code == null || !code.equals(cpUserUpdateEmailProfileVO.getCode() + "")){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("code error");
        }
        // 获取用户
        CPUser user = userDao.getById(session.getAttributeValue("uid",Long.class));
        user.setEmail(cpUserUpdateEmailProfileVO.getNewEmail());
        if (!userDao.save(user)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving user");
        }
        // 返回修改成功
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}
