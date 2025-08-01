package team.carrypigeon.backend.chat.domain.controller.group.member.authority;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.group.member.CPGroupMemberService;

@CPControllerTag("/core/group/member/authority")
public class CPGroupMemberAuthorityController implements CPController {

    private final ObjectMapper objectMapper;
    private final CPGroupMemberService cpGroupMemberService;

    public CPGroupMemberAuthorityController(ObjectMapper objectMapper, CPGroupMemberService cpGroupMemberService) {
        this.objectMapper = objectMapper;
        this.cpGroupMemberService = cpGroupMemberService;
    }


    @LoginPermission
    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPGroupMemberAuthorityVO cpGroupMemberAuthorityVO = objectMapper.treeToValue(data, CPGroupMemberAuthorityVO.class);
        return cpGroupMemberService.updateAuthority(cpGroupMemberAuthorityVO.getGid(), cpGroupMemberAuthorityVO.getUid(), cpGroupMemberAuthorityVO.getAuthority(), channel.getCPUserBO().getId());
    }
}
