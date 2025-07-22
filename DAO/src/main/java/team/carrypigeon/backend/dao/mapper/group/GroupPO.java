package team.carrypigeon.backend.dao.mapper.group;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("group")
public class GroupPO {
    @TableId
    private Long id;
    private String name;
    private Long owner;
    private String data;
    private LocalDateTime registerTime;
    private Long stateId;
}
