package team.carrypigeon.backend.dao.impl.friend;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.dao.friend.CPFriendDAO;
import team.carrypigeon.backend.api.bo.domain.friend.CPFriendBO;
import team.carrypigeon.backend.dao.mapper.friend.FriendMapper;
import team.carrypigeon.backend.dao.mapper.friend.FriendPO;

@Component
public class CPFriendImpl implements CPFriendDAO {

    private final FriendMapper friendMapper;

    public CPFriendImpl(FriendMapper friendMapper) {
        this.friendMapper = friendMapper;
    }

    @Override
    public long[] getAllFriends(long userId) {
        return new long[0];
    }

    @Override
    public CPFriendBO getFriend(long chanelId) {
        FriendPO friendPO = friendMapper.selectById(chanelId);
        if (friendPO == null) return null;
        return friendPO.toBO();
    }

    @Override
    public boolean acceptFriend(long user1, long user2) {
        //TODO
        return false;
    }

    @Override
    public boolean rejectFriend(long user1, long user2) {
        //TODO
        return false;
    }

    @Override
    public boolean applyFriend(long user1, long user2) {
        //TODO
        return false;
    }

    @Override
    public boolean deleteApply(long user1, long user2) {
        //TODO
        return false;
    }

    @Override
    public boolean deleteFriend(long user1, long user2) {
        //TODO
        return false;
    }
}
