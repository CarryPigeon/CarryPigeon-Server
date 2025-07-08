package team.carrypigeon.backend.dao.mapper.group.member;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("group_member")
public class GroupMemberPO {
    @TableId
    private long id;
    private long gid;
    private long uid;
    private int authority;
    private int state;
    private LocalDateTime time;
}
