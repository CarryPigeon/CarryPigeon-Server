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
    private Long id;
    private Long gid;
    private Long uid;
    private Integer authority;
    private Integer state;
    private LocalDateTime time;
}
