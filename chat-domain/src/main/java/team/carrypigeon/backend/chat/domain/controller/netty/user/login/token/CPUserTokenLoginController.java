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
 * 成功响应参数: {@link CPUserTokenLoginResult}<br/>
 * */
@CPControllerTag("/core/user/login/token")
public class CPUserTokenLoginController implements CPController {

    private final ObjectMapper objectMapper;

    private final UserTokenDao userTokenDao;

    private final CPSessionCenterService cpSessionCenterService;

    public CPUserTokenLoginController(ObjectMapper objectMapper, UserTokenDao userTokenDao, CPSessionCenterService cpSessionCenterService) {
        this.objectMapper = objectMapper;
        this.userTokenDao = userTokenDao;
        this.cpSessionCenterService = cpSessionCenterService;
    }

    @Override
    public CPResponse process(JsonNode data, CPSession session) {
        // 解析数据
        CPUserTokenLoginVO cpUserTokenLoginVO;
        try {
            cpUserTokenLoginVO = objectMapper.treeToValue(data, CPUserTokenLoginVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }

        // 从数据库获取用户
        CPUserToken userToken = userTokenDao.getByToken(cpUserTokenLoginVO.getToken());
        if (userToken == null) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("token does not exists");
        }
        String token = userToken.getToken();

        // 更新token
        userToken.setExpiredTime(userToken.getExpiredTime().plusDays(30))
                .setToken(IdUtil.generateToken());
        if (!userTokenDao.save(userToken)) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving token");
        }

        // 注册会话
        cpSessionCenterService.addSession(userToken.getUid(), session);
        // 将用户id注册进会话上下文
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, userToken.getUid());

        // 返回结果 最新的token与用户id
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPUserTokenLoginResult(token, userToken.getUid())));
    }
}