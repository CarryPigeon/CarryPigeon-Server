package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户通过token登录的结果
 * @author midreamsheep
 * */
@Data
@AllArgsConstructor
public class CPUserTokenLoginResult {
    // 刷新后的用户登录token
    private String token;
    // 用户uid
    private long uid;
}
