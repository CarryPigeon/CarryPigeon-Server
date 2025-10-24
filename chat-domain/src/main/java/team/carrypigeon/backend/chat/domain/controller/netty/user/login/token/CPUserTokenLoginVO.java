package team.carrypigeon.backend.chat.domain.controller.netty.user.login.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户通过token登录的参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserTokenLoginVO {
    // 用户登录token
    private String token;
}