package team.carrypigeon.backend.chat.domain.service.message.send;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageData;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageDomain;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageDomainEnum;
import team.carrypigeon.backend.chat.domain.manager.channel.nameToChatStructureManager;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

/**
 * 核心通讯结构文本类型消息的服务
 * */
@Service
public class CPCoreTextMessageService {

    private final CPMessageDAO cpMessageDAO;

    private final ObjectMapper objectMapper;

    private final nameToChatStructureManager nameToChatStructureManager;

    public CPCoreTextMessageService(CPMessageDAO cpMessageDAO, ObjectMapper objectMapper, nameToChatStructureManager nameToChatStructureManager) {
        this.cpMessageDAO = cpMessageDAO;
        this.objectMapper = objectMapper;
        this.nameToChatStructureManager = nameToChatStructureManager;
    }

    /**
     * 发送文本消息
     * */
    @SneakyThrows
    public CPResponse textMessageSend(long toId, String content, CPChannel channel, String typeName){
        // 权限校验
        CPChatStructure chatChannel = nameToChatStructureManager.getChannel(typeName);
        if(!chatChannel.verifyMember(toId, channel.getCPUserBO().getId())){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        // 存储数据结构
        CPMessageBO cpMessageBO = new CPMessageBO();
        cpMessageBO.setId(IdUtil.generateId());
        cpMessageBO.setSendUserId(channel.getCPUserBO().getId());
        cpMessageBO.setToId(toId);
        cpMessageBO.setDomain(new CPMessageDomain(CPMessageDomainEnum.CORE));
        CPMessageData cpMessageData = new CPMessageData();
        cpMessageData.setType(1);
        cpMessageData.setData(objectMapper.readValue("\"text\":\""+content+"\"",JsonNode.class));
        cpMessageBO.setData(cpMessageData);
        // 数据持久化
        cpMessageDAO.saveMessage(cpMessageBO);
        // 消息通知
        CPPacket cpNotification = new CPPacket();
        cpNotification.setRoute("/core/msg/text/send");
        cpNotification.setData(objectMapper.readValue(String.format("{\"mid\":%d,\"channel_id\":%d}",cpMessageBO.getId(),toId),JsonNode.class));
        chatChannel.noticeMember(toId,cpNotification);
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}
