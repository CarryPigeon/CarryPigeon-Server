package team.carrypigeon.backend.chat.domain.controller.group.member.apply;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.member.CPGroupMemberService;

@CPControllerTag("/core/group/member/apply")
public class CPGroupMemberApplyController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPGroupMemberService cpGroupMemberService;

    public CPGroupMemberApplyController(ObjectMapper objectMapper, CPGroupMemberService cpGroupMemberService) {
        this.objectMapper = objectMapper;
        this.cpGroupMemberService = cpGroupMemberService;
    }

    @Override
    @SneakyThrows
    @LoginPermission
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPGroupMemberApplyVO cpGroupMemberApplyVO = objectMapper.treeToValue(data, CPGroupMemberApplyVO.class);
        return cpGroupMemberService.createApply(cpGroupMemberApplyVO.getGid(), channel.getCPUserBO().getId());
    }
}
