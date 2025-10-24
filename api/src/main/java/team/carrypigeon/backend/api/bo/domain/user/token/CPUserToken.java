package team.carrypigeon.backend.api.bo.domain.user.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPUserToken {
    // token唯一id
    private long id;
    // 用户id
    private long uid;
    // token
    private String token;
    // 到期时间
    private LocalDateTime expiredTime;
}
