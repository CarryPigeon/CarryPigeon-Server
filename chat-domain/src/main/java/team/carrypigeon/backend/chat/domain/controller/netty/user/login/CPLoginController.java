package team.carrypigeon.backend.chat.domain.controller.netty.user.login;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.service.user.CPUserService;

@Slf4j
@CPControllerTag("/core/user/login")
public class CPLoginController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPUserService cpUserService;

    public CPLoginController(ObjectMapper objectMapper, CPUserService cpUserService) {
        this.objectMapper = objectMapper;
        this.cpUserService = cpUserService;
    }

    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPSession channel) {
        CPLoginVO loginVO = objectMapper.treeToValue(data, CPLoginVO.class);
        return  cpUserService.login(loginVO.getUid(), loginVO.getKey(), channel);
    }
}