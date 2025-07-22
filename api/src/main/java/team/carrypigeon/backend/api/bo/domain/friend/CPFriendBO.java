package team.carrypigeon.backend.api.bo.domain.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私聊通信结构BO类，由数据库中的好友结构映射而来
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPFriendBO {
    /// 私聊唯一id
    private long id;
    ///  用户1的id，为申请好友的用户
    private long user1;
    ///  用户2的id，为好友申请接收者
    private long user2;
    ///  申请状态，1：好友申请状态 2：好友已同意 3：好友已拒绝 4：好友已被删除
    private int state;
    ///  分状态时间，不同状态下时间映射含意不同，自行理解，以入库时间为准
    private LocalDateTime time;

    /**
     * 判断某人是否为处于任意好友状态的好友结构成员
     * */
    public boolean isMember(long userId) {
        return user1 == userId || user2 == userId;
    }

    /**
     * 判断某人是否为该好友结构的成员且两人为好友关系
     * */
    public boolean isFriend(long userId) {
        return isMember(userId) || state == 2;
    }
}
