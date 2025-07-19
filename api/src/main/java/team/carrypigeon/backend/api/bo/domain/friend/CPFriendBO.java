package team.carrypigeon.backend.api.bo.domain.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPFriendBO {
    private long id;
    private long user1;
    private long user2;
    private int state;
    private LocalDateTime time;

    public boolean isMember(long userId) {
        return user1 == userId || user2 == userId;
    }

    public boolean isFriend(long userId) {
        return isMember(userId) || state == 2;
    }
}
