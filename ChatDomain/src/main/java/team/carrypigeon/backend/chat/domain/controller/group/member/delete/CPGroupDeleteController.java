package team.carrypigeon.backend.chat.domain.controller.group.member.delete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.CPGroupService;

/**
 * 删除群组
 * */
@CPControllerTag("/core/group/delete")
public class CPGroupDeleteController implements CPController {

    private final CPGroupService cpGroupService;

    private final ObjectMapper objectMapper;

    public CPGroupDeleteController(CPGroupService cpGroupService, ObjectMapper objectMapper) {
        this.cpGroupService = cpGroupService;
        this.objectMapper = objectMapper;
    }


    @SneakyThrows
    @LoginPermission
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPGroupDeleteVO cpGroupDeleteVO = objectMapper.treeToValue(data, CPGroupDeleteVO.class);
        return cpGroupService.deleteGroup(cpGroupDeleteVO.getGid(), channel.getCPUserBO().getId());
    }
}
