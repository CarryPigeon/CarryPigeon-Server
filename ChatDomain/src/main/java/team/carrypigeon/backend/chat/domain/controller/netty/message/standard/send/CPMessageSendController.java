package team.carrypigeon.backend.chat.domain.controller.netty.message.standard.send;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageService;

@Slf4j
@CPControllerTag("/core/msg/send")
public class CPMessageSendController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPMessageService cpMessageService;

    public CPMessageSendController(ObjectMapper objectMapper, CPMessageService cpMessageService) {
        this.objectMapper = objectMapper;
        this.cpMessageService = cpMessageService;
    }


    @SneakyThrows
    @LoginPermission
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPMessageSendVO vo = objectMapper.treeToValue(data, CPMessageSendVO.class);
        log.debug("用户:{},发送消息：{}", channel.getCPUserBO().getId(), vo);
        return cpMessageService.send(channel, vo);
    }
}
