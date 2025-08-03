package team.carrypigeon.backend.chat.domain.controller.netty.group.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.CPGroupService;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取用户的所有群组
 * */
@CPControllerTag("/core/group/get")
public class CPGroupGetController implements CPController {

    private final CPGroupService cpGroupService;

    private final ObjectMapper objectMapper;

    public CPGroupGetController(CPGroupService cpGroupService, ObjectMapper objectMapper) {
        this.cpGroupService = cpGroupService;
        this.objectMapper = objectMapper;
    }

    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPChannel channel) {
        List<CPGroupGetReturn> userGroups = new ArrayList<>();
        for (CPGroupBO userGroup : cpGroupService.getUserGroups(channel.getCPUserBO().getId())) {
            userGroups.add(new CPGroupGetReturn(userGroup.getId(), userGroup.getStateId()));
        }
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.putPOJO("gids",userGroups);
        return CPResponse.SUCCESS_RESPONSE.setData(objectNode);
    }
}
