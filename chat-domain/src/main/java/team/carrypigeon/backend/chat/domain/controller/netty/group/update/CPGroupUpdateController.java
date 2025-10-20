package team.carrypigeon.backend.chat.domain.controller.netty.group.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.CPGroupService;

/**
 * 群聊数据更新
 * */
@CPControllerTag("/core/group/update")
public class CPGroupUpdateController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPGroupService cpGroupService;

    public CPGroupUpdateController(ObjectMapper objectMapper, CPGroupService cpGroupService) {
        this.objectMapper = objectMapper;
        this.cpGroupService = cpGroupService;
    }

    @LoginPermission
    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPSession channel) {
        CPGroupUpdateVO cpGroupUpdateVO = objectMapper.treeToValue(data, CPGroupUpdateVO.class);
        return cpGroupService.updateGroup(cpGroupUpdateVO.getGid(), channel.getCPUserBO().getId(), cpGroupUpdateVO.getName(), cpGroupUpdateVO.getIntroduction(), cpGroupUpdateVO.getProfile());
    }
}
