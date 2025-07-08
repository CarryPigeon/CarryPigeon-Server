package team.carrypigeon.backend.dao.mapper.friend;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("friend")
public class FriendPO {
    @TableId
    private long id;
    private long user1;
    private long user2;
    private int state;
    private LocalDateTime time;
}
