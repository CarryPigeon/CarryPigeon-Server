package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token.logout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;

/**
 * 用户登出的控制器<br/>
 * 登出请求url:/core/user/login/token/logout<br/>
 * 请求参数:{@link CPUserTokenLogoutVO} <br/>
 * 登出成功返回参数：无返回值<br/>
 * @author midreamsheep
 *  */
@CPControllerTag("/core/user/login/token/logout")
public class CPUserTokenLogoutController implements CPController {

    private final ObjectMapper objectMapper;

    private final UserTokenDao userTokenDao;

    public CPUserTokenLogoutController(ObjectMapper objectMapper, UserTokenDao userTokenDao) {
        this.objectMapper = objectMapper;
        this.userTokenDao = userTokenDao;
    }

    @Override
    @LoginPermission
    public CPResponse process(CPSession session, JsonNode data) {
        // 解析参数
        CPUserTokenLogoutVO cpUserTokenLogoutVO;
        try {
            cpUserTokenLogoutVO = objectMapper.treeToValue(data, CPUserTokenLogoutVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 调用删除方法
        if (!userTokenDao.delete(cpUserTokenLogoutVO.getToken())) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error deleting token");
        }
        // 登出成功
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}
