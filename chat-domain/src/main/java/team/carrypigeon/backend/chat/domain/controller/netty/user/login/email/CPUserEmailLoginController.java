package team.carrypigeon.backend.chat.domain.controller.netty.user.login.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.common.id.IdUtil;

/**
 * 用户注册的控制器<br/>
 * 请求url:/core/user/login/email<br/>
 * 请求参数:{@link CPUserEmailLoginVO}<br/>
 * 成功响应参数: {@link CPUserEmailLoginResult}<br/>
 * */
@CPControllerTag("/core/user/login/email")
public class CPUserEmailLoginController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPCache cache;

    private final UserDao userDao;

    private final UserTokenDao userTokenDao;

    public CPUserEmailLoginController(ObjectMapper objectMapper, CPCache cache, UserDao userDao, UserTokenDao userTokenDao) {
        this.objectMapper = objectMapper;
        this.cache = cache;
        this.userDao = userDao;
        this.userTokenDao = userTokenDao;
    }

    @Override
    public CPResponse process(JsonNode data, CPSession session) {
        // 数据解析
        CPUserEmailLoginVO cpUserEmailLoginVO;
        try {
            cpUserEmailLoginVO = objectMapper.treeToValue(data, CPUserEmailLoginVO.class);
        } catch (JsonProcessingException e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 判断数据有效性
        if (!cpUserEmailLoginVO.getEmail().matches("^[a-zA-Z0-9_+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("email error");
        }

        // 判断code是否正确
        String code = cache.getAndDelete(cpUserEmailLoginVO.getEmail() + ":code");
        if (code == null || !code.equals(cpUserEmailLoginVO.getCode() + "")){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("code error");
        }

        // 判断用户是否存在
        CPUser user = userDao.getByEmail(cpUserEmailLoginVO.getEmail());
        if (user == null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("user not exists");
        }

        // 创建用户token
        CPUserToken cpUserToken = new CPUserToken();
        cpUserToken.setId(IdUtil.generateId())
                .setToken(IdUtil.generateToken())
                .setUid(user.getId())
                .setExpiredTime(user.getRegisterTime().plusDays(30));
        if (!userTokenDao.save(cpUserToken)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving token");
        }
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPUserEmailLoginResult(cpUserToken.getToken())));
    }
}