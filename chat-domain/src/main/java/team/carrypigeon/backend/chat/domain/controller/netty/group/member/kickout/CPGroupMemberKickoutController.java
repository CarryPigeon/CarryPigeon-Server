package team.carrypigeon.backend.chat.domain.controller.netty.group.member.kickout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.member.CPGroupMemberService;

@CPControllerTag("/core/group/member/kickout")
public class CPGroupMemberKickoutController implements CPController {

    private final CPGroupMemberService cpGroupMemberService;

    private final ObjectMapper objectMapper;

    public CPGroupMemberKickoutController(CPGroupMemberService cpGroupMemberService, ObjectMapper objectMapper) {
        this.cpGroupMemberService = cpGroupMemberService;
        this.objectMapper = objectMapper;
    }

    @LoginPermission
    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPGroupMemberKickoutVO cpGroupMemberKickoutVO = objectMapper.treeToValue(data, CPGroupMemberKickoutVO.class);
        return null;
    }
}
