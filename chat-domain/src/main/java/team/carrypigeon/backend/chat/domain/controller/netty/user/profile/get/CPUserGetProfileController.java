package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 获取用户信息控制器<br/>
 * 请求url:/core/user/profile/get<br/>
 * 请求参数:{@link CPUserGetProfileVO}<br/>
 * 响应参数:{@link CPUserGetProfileResult}<br/>
 * */
@CPControllerTag("/core/user/profile/get")
public class CPUserGetProfileController implements CPController {

    private final ObjectMapper objectMapper;

    private final UserDao userDao;

    public CPUserGetProfileController(ObjectMapper objectMapper, UserDao userDao) {
        this.objectMapper = objectMapper;
        this.userDao = userDao;
    }

    @Override
    @LoginPermission
    public CPResponse process(CPSession session, JsonNode data) {
        // 参数解析
        CPUserGetProfileVO cpUserGetProfileVO;
        try {
            cpUserGetProfileVO = objectMapper.treeToValue(data, CPUserGetProfileVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }

        // 查询用户
        CPUser user = userDao.getById(cpUserGetProfileVO.getUid());
        if (user == null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("user not exists");
        }

        // 组装数据
        CPUserGetProfileResult cpUserGetProfileResult = new CPUserGetProfileResult();
        cpUserGetProfileResult.setSex(user.getSex().getValue())
                .setAvatar(user.getAvatar())
                .setBrief(user.getBrief())
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setBirthday(TimeUtil.LocalDateTimeToMillis(user.getBirthday()));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(cpUserGetProfileResult));
    }
}