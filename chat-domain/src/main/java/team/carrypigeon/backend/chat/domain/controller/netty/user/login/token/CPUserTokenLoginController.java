package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;
import team.carrypigeon.backend.common.id.IdUtil;

/**
 * 用户注册的控制器<br/>
 * 请求url:/core/user/login/token<br/>
 * 请求参数:{@link CPUserTokenLoginVO}<br/>
 * 成功响应参数: {@link CPUserTokenLoginResultNode}<br/>
 * */
@CPControllerTag(
        path = "/core/user/login/token", clazz = CPUserTokenLoginVO.class
)
public class CPUserTokenLoginController{}