package team.carrypigeon.backend.api.dao.friend;

/**
 * CarryPigeon 好友私聊相关接口
 * */
public interface CPFriendDAO {
    /**
     * 获取用户所有的好友
     * */
    long[] getAllFriends(long userId);
    /**
     * 同意用户的好友申请
     * 其中user1是申请发出者，user2是申请接受者
     * */
    boolean acceptFriend(long user1,long user2);
    /**
     * 同意用户的好友申请
     * 其中user1是申请发出者，user2是申请接受者
     * */
    boolean rejectFriend(long user1,long user2);
    /**
     * 提交好友申请
     * */
    boolean applyFriend(long user1,long user2);
    /**
     * 撤回好友申请
     * */
    boolean deleteApply(long user1,long user2);
    /**
     * 删除好友
     * */
    boolean deleteFriend(long user1,long user2);
}