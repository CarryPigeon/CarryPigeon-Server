package team.carrypigeon.backend.chat.domain.service.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.channel.ChatStructureTypeBO;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.api.dao.channel.CPChatStructureTypeDAO;
import team.carrypigeon.backend.api.dao.message.CPMessageDAO;
import team.carrypigeon.backend.api.bo.domain.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageBO;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageData;
import team.carrypigeon.backend.api.bo.domain.message.CPMessageDomain;
import team.carrypigeon.backend.chat.domain.controller.netty.message.standard.send.CPMessageSendVO;
import team.carrypigeon.backend.chat.domain.manager.structure.CPChatStructureManager;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.common.json.JsonNodeUtil;

/**
 * 核心通讯结构文本类型消息的服务
 * */
@Service
@Slf4j
public class CPMessageService {

    private final CPMessageDAO cpMessageDAO;

    private final CPChatStructureManager chatStructureManager;

    private final CPChatStructureTypeDAO cpChatStructureTypeDAO;

    public CPMessageService(CPMessageDAO cpMessageDAO, CPChatStructureManager cpChatStructureManager, CPChatStructureTypeDAO cpChatStructureTypeDAO) {
        this.cpMessageDAO = cpMessageDAO;
        this.chatStructureManager = cpChatStructureManager;
        this.cpChatStructureTypeDAO = cpChatStructureTypeDAO;
    }

    /**
     * 发送消息
     * */
    public CPResponse send(CPSession cpChannel, CPMessageSendVO cpMessageSendVO){
        // 权限校验,获取通讯结构
        ChatStructureTypeBO chatStructureType = cpChatStructureTypeDAO.getChatStructureType(cpMessageSendVO.getToId());
        if (chatStructureType == null) {
            log.error(cpMessageSendVO.toString());
            return CPResponse.ERROR_RESPONSE.copy().setTextData("illegal chat structure type");
        }
        // 验证是否成员
        CPChatStructure chatChannel = chatStructureManager.getChatStructure(chatStructureType.toStringData());
        if(!chatChannel.verifyMember(cpMessageSendVO.getToId(), cpChannel.getCPUserBO().getId())){
            return CPResponse.ERROR_RESPONSE.copy();
        }
        // 消息封装
        CPMessageBO cpMessageBO = new CPMessageBO()
                .setId(IdUtil.generateId())
                .setSendUserId(cpChannel.getCPUserBO().getId())
                .setToId(cpMessageSendVO.getToId())
                .setDomain(CPMessageDomain.fromDomain(cpMessageSendVO.getDomain()))
                .setData(new CPMessageData(cpMessageSendVO.getType(), cpMessageSendVO.getData()));
        // 消息持久化
        cpMessageDAO.saveMessage(cpMessageBO);
        // 消息通知
        CPPacket cpPacket = new CPPacket()
                .setRoute(cpMessageSendVO.getRoute())
                .setData(
                        JsonNodeUtil.createJsonNode(
                                "mid",cpMessageBO.getId()+"",
                                "cid", cpMessageSendVO.getToId()+""
                )
        );
        chatChannel.noticeMember(cpMessageSendVO.getToId(), cpPacket);
        // 返回成功值
        return CPResponse.SUCCESS_RESPONSE.copy();
    }

    public CPResponse delete(CPSession cpChannel, long messageId){
        // 获取消息
        CPMessageBO message = cpMessageDAO.getMessage(messageId);
        // 权限校验
        if(cpChannel.getCPUserBO().getId()!=message.getSendUserId()){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("illegal action");
        }
        // 时间校验,超过两分钟则拒绝请求
        if(System.currentTimeMillis()-message.getSendTime()>120000){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("over time message");
        }
        // 获取通讯结构
        CPChatStructure chatStructure = chatStructureManager.getChatStructure(cpChatStructureTypeDAO.getChatStructureType(message.getToId()).toStringData());
        // 操作持久化
        cpMessageDAO.deleteMessage(messageId,cpChannel.getCPUserBO().getId());
        // 通知所有人
        CPPacket cpPacket = new CPPacket()
                .setRoute("/core/msg/delete")
                .setData(
                        JsonNodeUtil.createJsonNode(
                                "mid",message.getId()+"",
                                "cid", message.getToId()+""
                        )
                );
        chatStructure.noticeMember(message.getToId(), cpPacket);
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}