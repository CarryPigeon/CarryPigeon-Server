package team.carrypigeon.backend.chat.domain.service.user;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.chat.domain.manager.channel.CPChannelManager;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

@Component
public class CPUserService {

    private final CPUserDAO userDAO;

    private final CPChannelManager userManager;

    public CPUserService(CPUserDAO userDAO, CPChannelManager userManager) {
        this.userDAO = userDAO;
        this.userManager = userManager;
    }

    public CPResponse login(long uid, String key, CPSession channel){
        String login = userDAO.login(uid, key);
        // 发生错误
        if (login == null) return CPResponse.ERROR_RESPONSE.copy();
        // 登录成功
        CPUserBO userBO = userDAO.getById(uid);
        // channel.setCPUserBO(userBO);
        userManager.addChannel(channel);
        return CPResponse.SUCCESS_RESPONSE.copy().setData(JsonNodeUtil.createJsonNode("token",login));
    }
}
