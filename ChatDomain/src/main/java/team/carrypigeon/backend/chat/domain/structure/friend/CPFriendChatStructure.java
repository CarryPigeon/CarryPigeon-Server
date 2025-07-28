package team.carrypigeon.backend.chat.domain.structure.friend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructure;
import team.carrypigeon.backend.api.chat.domain.structure.CPChatStructureTag;
import team.carrypigeon.backend.api.connection.vo.CPPacket;
import team.carrypigeon.backend.api.dao.friend.CPFriendDAO;
import team.carrypigeon.backend.api.bo.domain.friend.CPFriendBO;
import team.carrypigeon.backend.chat.domain.manager.user.CPUserToChannelManager;

/**
 * 私聊通信结构
 * */
@CPChatStructureTag("core:friend")
@Slf4j
public class CPFriendChatStructure implements CPChatStructure {

    private final CPFriendDAO cpFriendDAO;

    private final CPUserToChannelManager cpUserManager;

    private final ObjectMapper objectMapper;

    public CPFriendChatStructure(CPFriendDAO cpFriendDAO, CPUserToChannelManager cpUserManager, ObjectMapper objectMapper) {
        this.cpFriendDAO = cpFriendDAO;
        this.cpUserManager = cpUserManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean verifyMember(long channelId, long userId) {
        CPFriendBO friend = cpFriendDAO.getFriend(channelId);
        return friend != null && friend.isFriend(userId);
    }

    /**
     * 通知成员
     * 当有新消息或系统通知时，调用此方法向频道成员发送通知
     *
     * @param channelId 频道ID，用于获取频道相关信息
     * @param packet 消息包，包含要发送的通知信息
     * @return 总是返回true，表示通知过程已完成，不保证通知成功
     */
    @Override
    public boolean noticeMember(long channelId, CPPacket packet) {
        // 根据频道ID获取好友信息，包括两个用户
        CPFriendBO friend = cpFriendDAO.getFriend(channelId);
        // 创建一个包含两个用户的数组
        long[] users = new long[]{friend.getUser1(), friend.getUser2()};
        // 遍历用户数组，向每个用户发送通知
        for (long user : users) {
            // 获取用户的所有连接通道，并向每个通道发送消息
            cpUserManager.getChannels(user).forEach(channel -> {
                // 将消息包转换为JSON字符串并发送到频道
                try {
                    channel.sendMessage(objectMapper.writeValueAsString(packet));
                } catch (JsonProcessingException e) {
                    // 如果JSON处理出错，记录错误日志
                    log.error("Json数据处理出错，json结构：{}",packet.toString());
                    log.error(e.getMessage(), e);
                }
            });
        }
        // 通知过程完成，返回true
        return true;
    }
}
