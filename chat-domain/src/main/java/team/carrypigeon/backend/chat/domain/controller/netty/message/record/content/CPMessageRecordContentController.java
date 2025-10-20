package team.carrypigeon.backend.chat.domain.controller.netty.message.record.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;
import team.carrypigeon.backend.chat.domain.manager.structure.CPChatStructureManager;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * 通过消息id获取服务端消息内容
 * */
@Slf4j
@CPControllerTag("/core/msg/get/content")
public class CPMessageRecordContentController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPMessageDAO cpMessageDAO;

    private final CPChatStructureManager cpChatStructureManager;

    public CPMessageRecordContentController(ObjectMapper objectMapper, CPMessageDAO cpMessageDAO, CPChatStructureManager cpChatStructureManager) {
        this.objectMapper = objectMapper;
        this.cpMessageDAO = cpMessageDAO;
        this.cpChatStructureManager = cpChatStructureManager;
    }


    @SneakyThrows
    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPSession channel) {
        CPMessageRecordContentVO cpMessageRecordContentVO = objectMapper.treeToValue(data, CPMessageRecordContentVO.class);
        List<JsonNode> messageList = new ArrayList<>();
        for (long mid : cpMessageRecordContentVO.getMids()) {
            // 权限校验
            CPMessageBO message = cpMessageDAO.getMessage(mid);
            if (!cpChatStructureManager.getChatStructure(message.getToId()).verifyMember(message.getToId(), channel.getCPUserBO().getId())){
                continue;
            }
            messageList.add(message.toJsonData(objectMapper));
        }
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.putPOJO("messages",messageList);
        return CPResponse.SUCCESS_RESPONSE.setData(objectNode);
    }
}
