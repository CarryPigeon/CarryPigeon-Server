package team.carrypigeon.backend.chat.domain.controller.user.account.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.user.account.CPAccountService;

@CPControllerTag("/core/account/edit")
public class CPAccountEditController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPAccountService cpAccountService;

    public CPAccountEditController(ObjectMapper objectMapper, CPAccountService cpAccountService) {
        this.objectMapper = objectMapper;
        this.cpAccountService = cpAccountService;
    }

    @SneakyThrows
    @LoginPermission
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPAccountEditVO cpAccountEditVO = objectMapper.treeToValue(data, CPAccountEditVO.class);
        return cpAccountService.update(channel.getCPUserBO().getId(),cpAccountEditVO.getName(),cpAccountEditVO.getIntroduction(),cpAccountEditVO.getProfile());
    }
}
