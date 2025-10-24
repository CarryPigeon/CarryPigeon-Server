package team.carrypigeon.backend.chat.domain.controller.netty.user.login.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户邮箱登录请求数据
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserEmailLoginVO {
    // 用户邮箱
    private String email;
    // 验证码
    private int code;
}
