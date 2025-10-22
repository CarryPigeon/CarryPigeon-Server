package team.carrypigeon.backend.dao.database.mapper.user.token;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_token")
public class UserTokenPO {
    // 令牌id
    @TableId
    private Long id;
    // 用户id
    private Long uid;
    // 令牌
    private String token;
    // 令牌到期时间
    private LocalDateTime expiredTime;
}
