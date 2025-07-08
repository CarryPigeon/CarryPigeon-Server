package team.carrypigeon.backend.dao.mapper.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPO {
    @TableId
    private long id;
    private String name;
    private String email;
    private String password;
    private String data;
    private LocalDateTime registerTime;
    private long stateId;
}
