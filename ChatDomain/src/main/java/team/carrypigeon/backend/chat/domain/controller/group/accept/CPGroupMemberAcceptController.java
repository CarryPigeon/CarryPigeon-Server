package team.carrypigeon.backend.chat.domain.controller.group.accept;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.member.CPGroupMemberService;

@CPControllerTag("/core/group/member/apply/accept")
public class CPGroupMemberAcceptController implements CPController {

    private final CPGroupMemberService cpGroupMemberService;

    private final ObjectMapper objectMapper;

    public CPGroupMemberAcceptController(CPGroupMemberService cpGroupMemberService, ObjectMapper objectMapper) {
        this.cpGroupMemberService = cpGroupMemberService;
        this.objectMapper = objectMapper;
    }

    @LoginPermission
    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPGroupMemberAcceptVO cpGroupMemberAcceptVO = objectMapper.treeToValue(data, CPGroupMemberAcceptVO.class);
        return cpGroupMemberService.acceptApply(cpGroupMemberAcceptVO.getGid(), channel.getCPUserBO().getId(), cpGroupMemberAcceptVO.getUid());
    }
}
