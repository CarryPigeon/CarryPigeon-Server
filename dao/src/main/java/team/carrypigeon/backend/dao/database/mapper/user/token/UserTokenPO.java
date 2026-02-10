package team.carrypigeon.backend.dao.database.mapper.user.token;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken;

import java.time.LocalDateTime;

/**
 * `user_token` 表持久化对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("user_token")
public class UserTokenPO {

    /**
     * 令牌记录 ID。
     */
    @TableId
    private Long id;

    /**
     * 用户 ID。
     */
    private Long uid;

    /**
     * 刷新令牌字符串。
     */
    private String token;

    /**
     * 到期时间。
     */
    private LocalDateTime expiredTime;

    /**
     * 将 PO 转换为 BO。
     *
     * @return 用户令牌领域对象。
     */
    public CPUserToken toBo() {
        return new CPUserToken(id, uid, token, expiredTime);
    }

    /**
     * 从 BO 构建 PO。
     *
     * @param cpUserToken 用户令牌领域对象。
     * @return 用户令牌持久化对象。
     */
    public static UserTokenPO from(CPUserToken cpUserToken) {
        return new UserTokenPO()
                .setId(cpUserToken.getId())
                .setUid(cpUserToken.getUid())
                .setToken(cpUserToken.getToken())
                .setExpiredTime(cpUserToken.getExpiredTime());
    }
}
