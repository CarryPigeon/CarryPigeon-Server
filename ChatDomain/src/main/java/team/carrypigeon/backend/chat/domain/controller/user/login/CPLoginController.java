package team.carrypigeon.backend.chat.domain.controller.user.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.dao.user.CPUserDAO;
import team.carrypigeon.backend.api.domain.CPChannel;
import team.carrypigeon.backend.api.domain.bo.user.CPUserBO;
import team.carrypigeon.backend.chat.domain.manager.user.CPUserManager;
import team.carrypigeon.backend.common.response.CPResponse;

@Slf4j
@CPControllerTag("/core/account/login")
public class CPLoginController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPUserDAO userDAO;

    private final CPUserManager userManager;

    public CPLoginController(ObjectMapper objectMapper, CPUserDAO userDAO, CPUserManager userManager) {
        this.objectMapper = objectMapper;
        this.userDAO = userDAO;
        this.userManager = userManager;
    }

    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        try {
            CPLoginVO loginVO = objectMapper.treeToValue(data, CPLoginVO.class);
            CPUserBO userBO = userDAO.login(loginVO.getEmail(), loginVO.getPassword());
            System.out.println("login ok");
            if(userBO != null){
                // 进行用户注册操作
                channel.setCPUserBO(userBO);
                userManager.addChannel(channel);
            }
            CPResponse response = new CPResponse();
            response.setCode(200);
            return  response;
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(),e);
        }
        CPResponse response = new CPResponse();
        response.setCode(100);
        return  response;
    }
}