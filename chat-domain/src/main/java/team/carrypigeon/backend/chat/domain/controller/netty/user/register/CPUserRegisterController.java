package team.carrypigeon.backend.chat.domain.controller.netty.user.register;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 用户注册的控制器<br/>
 * 请求url:/core/user/register<br/>
 * 请求参数:{@link CPUserRegisterVO}<br/>
 * 成功响应参数: {@link CPUserRegisterResult}<br/>
 * */
@CPControllerTag("/core/user/register")
public class CPUserRegisterController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPCache cache;

    private final UserDao userDAO;

    private final UserTokenDao userTokenDAO;

    public CPUserRegisterController(ObjectMapper objectMapper, CPCache cache, UserDao userDAO, UserTokenDao userTokenDAO) {
        this.objectMapper = objectMapper;
        this.cache = cache;
        this.userDAO = userDAO;
        this.userTokenDAO = userTokenDAO;
    }

    @Override
    public CPResponse process(CPSession session, JsonNode data) {
        // 参数校验
        CPUserRegisterVO vo;
        try {
            vo = objectMapper.treeToValue(data, CPUserRegisterVO.class);
        } catch (JsonProcessingException e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 业务逻辑处理
        // 判断code是否正确
        String code = cache.getAndDelete(vo.getEmail() + ":code");
        if (code == null || !code.equals(vo.getCode() + "")){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("code error");
        }
        // 校验邮箱是否正确
        if (!vo.getEmail().matches("^[a-zA-Z0-9_+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("email error");
        }
        // 判断用户是否存在
        if (userDAO.getByEmail(vo.getEmail()) != null) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("user already exists");
        }
        // 创建用户
        CPUser newUser = new CPUser();
        newUser.setId(IdUtil.generateId())
                .setEmail(vo.getEmail())
                .setBirthday(null)
                .setAvatar(-1)
                .setSex(CPUserSexEnum.UNKNOWN)
                .setBrief("")
                .setUsername(newUser.getId()+"")
                .setRegisterTime(TimeUtil.getCurrentLocalTime());
        if (!userDAO.save(newUser)) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving user");
        }
        // 创建token
        CPUserToken newToken = new CPUserToken();
        newToken.setId(IdUtil.generateId())
                .setUid(newUser.getId())
                .setToken(IdUtil.generateToken())
                .setExpiredTime(TimeUtil.getCurrentLocalTime().plusDays(30));
        if (!userTokenDAO.save(newToken)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving token");
        }
        // 返回成功的结果
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPUserRegisterResult(newToken.getToken())));
    }
}
