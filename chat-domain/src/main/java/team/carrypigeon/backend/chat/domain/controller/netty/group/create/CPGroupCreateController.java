package team.carrypigeon.backend.chat.domain.controller.netty.group.create;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.CPGroupService;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

/**
 * 创建私有群居
 * TODO 实现配置文件决定是否开放功能
 * */
@CPControllerTag("/core/group/create")
public class CPGroupCreateController implements CPController {

    private final CPGroupService cpGroupService;

    public CPGroupCreateController(CPGroupService cpGroupService) {
        this.cpGroupService = cpGroupService;
    }

    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPChannel channel) {
        long group = cpGroupService.createGroup(channel.getCPUserBO().getId());
        JsonNode gid = JsonNodeUtil.createJsonNode("gid", group + "");
        if (group != -1){
            return CPResponse.SUCCESS_RESPONSE.copy().setData(gid);
        }else {
            return CPResponse.ERROR_RESPONSE.copy().setData(gid);
        }
    }
}
