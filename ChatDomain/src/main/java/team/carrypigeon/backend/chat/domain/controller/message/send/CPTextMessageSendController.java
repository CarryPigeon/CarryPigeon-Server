package team.carrypigeon.backend.chat.domain.controller.message.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.dao.channel.CPChannelTypeDAO;
import team.carrypigeon.backend.api.domain.CPChannel;
import team.carrypigeon.backend.api.domain.bo.channel.ChannelTypeBO;
import team.carrypigeon.backend.chat.domain.service.message.send.CPCoreTextMessageService;
import team.carrypigeon.backend.common.response.CPResponse;

/**
 * 消息发送处理器，用于处理消息发送事件
 * */
@Slf4j
@CPControllerTag("/core/msg/text/send")
public class CPTextMessageSendController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPChannelTypeDAO cpChannelTypeDAO;

    private final CPCoreTextMessageService cpCoreTextMessageService;

    public CPTextMessageSendController(ObjectMapper objectMapper, CPChannelTypeDAO cpChannelTypeDAO, CPCoreTextMessageService cpCoreTextMessageService) {
        this.objectMapper = objectMapper;
        this.cpChannelTypeDAO = cpChannelTypeDAO;
        this.cpCoreTextMessageService = cpCoreTextMessageService;
    }

    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        // 解析数据
        CPMessageSendVO vo;
        try {
            vo = objectMapper.treeToValue(data, CPMessageSendVO.class);
            log.debug(vo.toString());
        } catch (JsonProcessingException e) {
            // TODO 错误处理
            log.error(e.getMessage(),e);
            return null;
        }
        // 类型获取
        ChannelTypeBO channelType = cpChannelTypeDAO.getChannelType(vo.getTo_id());
        if (channelType == null) {
            // TODO 错误处理
            log.error(vo.toString());
            return null;
        }
        switch (channelType.getType()){
            case CORE:
                return cpCoreTextMessageService.textMessageSend(vo.getTo_id(),vo.getContent(), channel,channelType.getTypeName());
            case PLUGINS:
                break;
        }
        return null;
    }
}