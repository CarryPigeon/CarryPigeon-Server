package team.carrypigeon.backend.chat.domain.controller.netty.group.dataget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.CPGroupService;

import java.util.LinkedList;
import java.util.List;

/**
 * 获取群组具体数据
 * */
@CPControllerTag("/core/group/data/get")
public class CPGroupDataGetController implements CPController {

    private final CPGroupService cpGroupService;

    private final ObjectMapper objectMapper;

    public CPGroupDataGetController(CPGroupService cpGroupService, ObjectMapper objectMapper) {
        this.cpGroupService = cpGroupService;
        this.objectMapper = objectMapper;
    }

    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPChannel channel) {
        long gid = data.get("gid").asLong();
        CPGroupBO group = cpGroupService.getGroupById(gid);
        CPGroupMemberBO[] membersByGroupId = cpGroupService.getMembersByGroupId(gid);
        boolean isMember = false;
        List<Long> members = new LinkedList<>();
        for (CPGroupMemberBO member : membersByGroupId) {
            if (member.getUid() == channel.getCPUserBO().getId()) {
                isMember = true;
            }
            members.add(member.getUid());
        }
        if (!isMember) return CPResponse.ERROR_RESPONSE.copy().setTextData("not group member");
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("name",group.getName())
                .put("owner",group.getOwner())
                .put("introduction",group.getIntroduction())
                .put("profile",group.getProfile())
                .put("register_time",group.getRegisterTime())
                .put("state_id",group.getStateId())
                .putPOJO("members",members);
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectNode);
    }
}
