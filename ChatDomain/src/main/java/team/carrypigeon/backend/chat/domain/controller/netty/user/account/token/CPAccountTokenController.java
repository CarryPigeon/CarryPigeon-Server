package team.carrypigeon.backend.chat.domain.controller.netty.user.account.token;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.service.user.account.CPAccountService;

@CPControllerTag("/core/account/key")
public class CPAccountTokenController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPAccountService cpAccountService;

    public CPAccountTokenController(ObjectMapper objectMapper, CPAccountService cpAccountService) {
        this.objectMapper = objectMapper;
        this.cpAccountService = cpAccountService;
    }

    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPAccountTokenVO cpAccountKeyVO = objectMapper.treeToValue(data, CPAccountTokenVO.class);
        return cpAccountService.login(cpAccountKeyVO.getEmail(), cpAccountKeyVO.getPassword(),cpAccountKeyVO.getCode(),cpAccountKeyVO.getDeviceName());
    }
}
