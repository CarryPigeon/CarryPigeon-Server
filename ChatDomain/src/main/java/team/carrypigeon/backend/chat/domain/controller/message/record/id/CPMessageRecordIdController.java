package team.carrypigeon.backend.chat.domain.controller.message.record.id;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;

import java.time.LocalDateTime;

/**
 * 获取指定消息的时间之前的特定数量的消息
 * */
@Slf4j
@CPControllerTag("/core/msg/get/id")
public class CPMessageRecordIdController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPMessageDAO cpMessageDAO;

    public CPMessageRecordIdController(ObjectMapper objectMapper, CPMessageDAO cpMessageDAO) {
        this.objectMapper = objectMapper;
        this.cpMessageDAO = cpMessageDAO;
    }


    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        System.out.println(System.currentTimeMillis());
        CPMessageRecordIdVO cpMessageRecordIdVO = objectMapper.treeToValue(data, CPMessageRecordIdVO.class);
        Long[] ids = cpMessageDAO.getMessageFromTime(cpMessageRecordIdVO.getChannelId(), cpMessageRecordIdVO.getFromTime(), cpMessageRecordIdVO.getCount());
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("count",ids.length);
        objectNode.putPOJO("mids",ids);
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectNode);
    }
}
