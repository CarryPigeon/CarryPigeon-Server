package team.carrypigeon.backend.chat.domain.controller.user.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;
import team.carrypigeon.backend.chat.domain.manager.user.CPUserToChannelManager;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

@Slf4j
@CPControllerTag("/core/account/login")
public class CPLoginController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPUserDAO userDAO;

    private final CPUserToChannelManager userManager;

    public CPLoginController(ObjectMapper objectMapper, CPUserDAO userDAO, CPUserToChannelManager userManager) {
        this.objectMapper = objectMapper;
        this.userDAO = userDAO;
        this.userManager = userManager;
    }

    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPLoginVO loginVO = objectMapper.treeToValue(data, CPLoginVO.class);
        CPUserBO userBO = userDAO.login(loginVO.getEmail(), loginVO.getPassword());
        if(userBO == null) return CPResponse.ERROR_RESPONSE.copy();
        channel.setCPUserBO(userBO);
        userManager.addChannel(channel);
        return  CPResponse.SUCCESS_RESPONSE.copy();
    }
}