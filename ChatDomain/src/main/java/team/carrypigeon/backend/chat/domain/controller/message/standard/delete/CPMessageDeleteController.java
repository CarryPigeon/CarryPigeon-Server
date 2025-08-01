package team.carrypigeon.backend.chat.domain.controller.message.standard.delete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageService;

@CPControllerTag("/core/msg/delete")
public class CPMessageDeleteController implements CPController {

    private final ObjectMapper mapper;

    private final CPMessageService cpMessageService;

    public CPMessageDeleteController(ObjectMapper mapper, CPMessageService cpMessageService) {
        this.mapper = mapper;
        this.cpMessageService = cpMessageService;
    }

    @SneakyThrows
    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPChannel channel) {
        // 数据转换
        CPMessageDeleteVO cpMessageDeleteVO = mapper.treeToValue(data, CPMessageDeleteVO.class);
        return cpMessageService.delete(channel,cpMessageDeleteVO.getMid());
    }
}
