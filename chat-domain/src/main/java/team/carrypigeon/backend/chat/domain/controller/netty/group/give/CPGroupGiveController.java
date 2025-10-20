package team.carrypigeon.backend.chat.domain.controller.netty.group.give;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.CPGroupService;

@CPControllerTag("/core/group/give")
public class CPGroupGiveController implements CPController {
    private final ObjectMapper objectMapper;

    private final CPGroupService cpGroupService;

    public CPGroupGiveController(ObjectMapper objectMapper, CPGroupService cpGroupService) {
        this.objectMapper = objectMapper;
        this.cpGroupService = cpGroupService;
    }

    @SneakyThrows
    @LoginPermission
    @Override
    public CPResponse process(JsonNode data, CPSession channel) {
        CPGroupGiveVO cpGroupGiveVO = objectMapper.treeToValue(data, CPGroupGiveVO.class);
        return cpGroupService.giveGroup(cpGroupGiveVO.getGid(), channel.getCPUserBO().getId(), cpGroupGiveVO.getUid());
    }
}
