package team.carrypigeon.backend.dao.mapper.group;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.group.CPGroupBO;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("`group`")
public class GroupPO {
    @TableId
    private Long id;
    private String name;
    private Long owner;
    private String introduction;
    private Long profile;
    private LocalDateTime registerTime;
    private Long stateId;

    public CPGroupBO toGroupBO(){
        return new CPGroupBO(
                id,
                name,
                owner,
                introduction,
                profile,
                registerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                stateId
        );
    }

    public void fillData(CPGroupBO groupBO){
        this.id = groupBO.getId();
        this.name = groupBO.getName();
        this.owner = groupBO.getOwner();
        this.introduction = groupBO.getIntroduction();
        this.profile = groupBO.getProfile();
        if (groupBO.getRegisterTime() != 0){
            this.registerTime = LocalDateTimeUtil.of(groupBO.getRegisterTime());
        }
        this.stateId = groupBO.getStateId();
    }
}
