package team.carrypigeon.backend.api.bo.domain.user.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Token 领域对象：表示用户登录 token 及其生命周期。
 * <p>
 * 该对象通常由 DAO 层持久化，并用于 token 登录、注销与过期校验。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPUserToken {
    /**
     * token 唯一 ID（数据库主键或雪花 ID）。
     */
    private long id;
    /**
     * 用户 ID。
     */
    private long uid;
    /**
     * token 字符串值。
     */
    private String token;
    /**
     * 到期时间（服务端本地时间）。
     */
    private LocalDateTime expiredTime;
}
