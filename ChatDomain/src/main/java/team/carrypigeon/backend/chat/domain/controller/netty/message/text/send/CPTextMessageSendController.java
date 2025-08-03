package team.carrypigeon.backend.chat.domain.controller.netty.message.text.send;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.chat.domain.controller.netty.message.standard.send.CPMessageSendController;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

/**
 * 消息发送处理器，用于处理消息发送事件
 * */
@Slf4j
@CPControllerTag("/core/msg/text/send")
public class CPTextMessageSendController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPMessageSendController cpMessageSendController;


    public CPTextMessageSendController(ObjectMapper objectMapper, CPMessageSendController cpMessageSendController) {
        this.objectMapper = objectMapper;
        this.cpMessageSendController = cpMessageSendController;
    }

    @SneakyThrows
    @LoginPermission
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        // 解析数据
        CPTextMessageSendVO vo = objectMapper.treeToValue(data, CPTextMessageSendVO.class);
        // 重组为cpMessageSendController所需的数据
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("to_id",vo.getToId());
        objectNode.put("domain","core");
        objectNode.put("type",1);
        objectNode.putPOJO("data", JsonNodeUtil.createJsonNode("text",vo.getText()));
        // 类型获取
        return cpMessageSendController.process(objectNode,channel);
    }
}