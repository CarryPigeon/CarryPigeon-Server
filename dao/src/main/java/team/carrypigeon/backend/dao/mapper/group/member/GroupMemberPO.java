package team.carrypigeon.backend.dao.mapper.group.member;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.group.member.CPGroupMemberBO;

import java.time.LocalDateTime;
import java.time.ZoneId;

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

    public CPGroupMemberBO toBO(){
        return new CPGroupMemberBO(id, gid, uid, authority, state, time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    public void fillData(CPGroupMemberBO groupMemberBO){
        this.id = groupMemberBO.getId();
        this.gid = groupMemberBO.getGid();
        this.uid = groupMemberBO.getUid();
        this.authority = groupMemberBO.getAuthority();
        this.state = groupMemberBO.getState();
        if (groupMemberBO.getTime() != 0){
            time = LocalDateTimeUtil.of(groupMemberBO.getTime());
        }
    }
}
