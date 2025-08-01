package team.carrypigeon.backend.chat.domain.controller.group.member.reject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.member.CPGroupMemberService;

@CPControllerTag("/core/group/member/apply/reject")
public class CPGroupMemberRejectController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPGroupMemberService cpGroupMemberService;

    public CPGroupMemberRejectController(ObjectMapper objectMapper, CPGroupMemberService cpGroupMemberService) {
        this.objectMapper = objectMapper;
        this.cpGroupMemberService = cpGroupMemberService;
    }


    @LoginPermission
    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPGroupMemberRejectVO cpGroupMemberRejectVO = objectMapper.treeToValue(data, CPGroupMemberRejectVO.class);
        return cpGroupMemberService.rejectApply(cpGroupMemberRejectVO.getGid(), cpGroupMemberRejectVO.getUid(), channel.getCPUserBO().getId());
    }
}
