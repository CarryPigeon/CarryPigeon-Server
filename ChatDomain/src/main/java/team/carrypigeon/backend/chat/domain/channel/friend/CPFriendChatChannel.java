package team.carrypigeon.backend.chat.domain.channel.friend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.channel.CPChatChannel;
import team.carrypigeon.backend.api.chat.domain.channel.CPChatChannelTag;
import team.carrypigeon.backend.api.dao.friend.CPFriendDAO;
import team.carrypigeon.backend.api.bo.domain.friend.CPFriendBO;
import team.carrypigeon.backend.api.vo.CPNotification;
import team.carrypigeon.backend.chat.domain.manager.user.CPUserManager;

@CPChatChannelTag("friend")
@Slf4j
public class CPFriendChatChannel implements CPChatChannel {

    private final CPFriendDAO cpFriendDAO;

    private final CPUserManager cpUserManager;

    private final ObjectMapper objectMapper;

    public CPFriendChatChannel(CPFriendDAO cpFriendDAO, CPUserManager cpUserManager, ObjectMapper objectMapper) {
        this.cpFriendDAO = cpFriendDAO;
        this.cpUserManager = cpUserManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean verifyMember(long channelId, long userId) {
        CPFriendBO friend = cpFriendDAO.getFriend(channelId);
        return friend != null && friend.isFriend(userId);
    }

    @Override
    public boolean noticeMember(long channelId, CPNotification notification) {
        CPFriendBO friend = cpFriendDAO.getFriend(channelId);
        long[] users = new long[]{friend.getUser1(), friend.getUser2()};
        System.out.println(users);
        for (long user : users) {
            cpUserManager.getChannels(user).forEach(channel -> {
                try {
                    channel.sendMessage(objectMapper.writeValueAsString(notification));
                    log.info("消息发送给{},内容为{}",user,objectMapper.writeValueAsString(notification));
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
        return true;
    }
}
