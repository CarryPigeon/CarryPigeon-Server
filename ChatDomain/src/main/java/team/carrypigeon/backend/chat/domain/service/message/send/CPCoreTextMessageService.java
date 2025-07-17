package team.carrypigeon.backend.chat.domain.service.message.send;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;
import team.carrypigeon.backend.api.domain.CPChannel;
import team.carrypigeon.backend.api.domain.bo.channel.ChannelCoreTypeMenu;
import team.carrypigeon.backend.api.domain.bo.message.CPMessageBO;
import team.carrypigeon.backend.api.domain.bo.message.CPMessageData;
import team.carrypigeon.backend.api.domain.bo.message.CPMessageDomain;
import team.carrypigeon.backend.api.domain.bo.message.CPMessageDomainEnum;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.response.CPResponse;

/**
 * 核心通讯结构文本类型消息的服务
 * */
@Service
public class CPCoreTextMessageService {

    private final CPMessageDAO cpMessageDAO;

    private final ObjectMapper objectMapper;

    public CPCoreTextMessageService(CPMessageDAO cpMessageDAO, ObjectMapper objectMapper) {
        this.cpMessageDAO = cpMessageDAO;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送文本消息
     * */
    @SneakyThrows
    public CPResponse textMessageSend(long toId, String content, CPChannel channel, String typeName){
        // 权限校验
        ChannelCoreTypeMenu channelCoreTypeMenu = ChannelCoreTypeMenu.valueOfByName(typeName);
        switch (channelCoreTypeMenu){
            case FRIEND:
                break;
            case null:
                break;
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
        CPResponse response = new CPResponse();
        response.setCode(200);
        return response;
    }
}
