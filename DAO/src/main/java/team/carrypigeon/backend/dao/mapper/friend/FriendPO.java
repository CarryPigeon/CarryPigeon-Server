package team.carrypigeon.backend.dao.mapper.friend;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import team.carrypigeon.backend.api.bo.domain.friend.CPFriendBO;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@TableName("friend")
public class FriendPO {
    @TableId
    private long id;
    @TableField("user_1")
    private long user1;
    @TableField("user_2")
    private long user2;
    private int state;
    private LocalDateTime time;

    public CPFriendBO toBO(){
        System.out.println(this);
        return new CPFriendBO(id, user1, user2, state, time);
    }

    public void fromBO(CPFriendBO bo){
        this.id = bo.getId();
        this.user1 = bo.getUser1();
        this.user2 = bo.getUser2();
        this.state = bo.getState();
        this.time = bo.getTime();
    }
}
