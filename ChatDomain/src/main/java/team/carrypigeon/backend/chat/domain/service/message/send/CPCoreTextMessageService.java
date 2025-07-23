package team.carrypigeon.backend.chat.domain.service.message.send;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageData;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageDomain;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageDomainEnum;
import team.carrypigeon.backend.chat.domain.manager.channel.NameToChatStructureManager;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

/**
 * 核心通讯结构文本类型消息的服务
 * */
@Service
@Slf4j
public class CPCoreTextMessageService {

    private final CPMessageDAO cpMessageDAO;

    private final NameToChatStructureManager nameToChatStructureManager;

    public CPCoreTextMessageService(CPMessageDAO cpMessageDAO, NameToChatStructureManager nameToChatStructureManager) {
        this.cpMessageDAO = cpMessageDAO;
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
        CPMessageBO cpMessageBO = new CPMessageBO()
                .setId(IdUtil.generateId())
                .setSendUserId(channel.getCPUserBO().getId())
                .setToId(toId)
                .setDomain(new CPMessageDomain(CPMessageDomainEnum.CORE));
        CPMessageData cpMessageData = new CPMessageData()
                .setType(1)
                .setData(JsonNodeUtil.createJsonNode("text",content));
        cpMessageBO.setData(cpMessageData);
        // 消息持久化
        cpMessageDAO.saveMessage(cpMessageBO);
        // 消息通知
        CPPacket cpPacket = new CPPacket()
                .setRoute("/core/msg/text/send");
        cpPacket.setData(
                JsonNodeUtil.createJsonNode(
                        "mid",cpMessageBO.getId()+"",
                        "channel_id",toId+""
                )
        );
        chatChannel.noticeMember(toId,cpPacket);
        // 返回成功值
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}