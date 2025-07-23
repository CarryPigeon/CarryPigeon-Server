package team.carrypigeon.backend.chat.domain.controller.message.send;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.dao.channel.CPChatStructureTypeDAO;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.ChatStructureTypeBO;
import team.carrypigeon.backend.chat.domain.service.message.send.CPCoreTextMessageService;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

/**
 * 消息发送处理器，用于处理消息发送事件
 * */
@Slf4j
@CPControllerTag("/core/msg/text/send")
public class CPTextMessageSendController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPChatStructureTypeDAO cpChatStructureTypeDAO;

    private final CPCoreTextMessageService cpCoreTextMessageService;

    public CPTextMessageSendController(ObjectMapper objectMapper, CPChatStructureTypeDAO cpChatStructureTypeDAO, CPCoreTextMessageService cpCoreTextMessageService) {
        this.objectMapper = objectMapper;
        this.cpChatStructureTypeDAO = cpChatStructureTypeDAO;
        this.cpCoreTextMessageService = cpCoreTextMessageService;
    }

    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        // 解析数据
        CPMessageSendVO vo = objectMapper.treeToValue(data, CPMessageSendVO.class);
        // 类型获取
        ChatStructureTypeBO chatStructureType = cpChatStructureTypeDAO.getChatStructureType(vo.getToId());
        if (chatStructureType == null) {
            log.error(vo.toString());
            return CPResponse.ERROR_RESPONSE.copy()
                    .setTextData("illegal chat structure type");
        }
        switch (chatStructureType.getType()){
            case CORE:
                return cpCoreTextMessageService.textMessageSend(vo.getToId(),vo.getContent(), channel,chatStructureType.getTypeName());
            case PLUGINS:
                break;
        }
        return CPResponse.ERROR_RESPONSE.copy()
                .setTextData("illegal chat structure type");
    }
}