package team.carrypigeon.backend.api.dao.channel;

import team.carrypigeon.backend.api.bo.domain.channel.ChatStructureTypeBO;

/**
 * channel_type的dao接口
 * */
public interface CPChatStructureTypeDAO {
    /**
     * 通过通道id获取通道类型的BO结构
     * */
    ChatStructureTypeBO getChatStructureType(long chatId);
}