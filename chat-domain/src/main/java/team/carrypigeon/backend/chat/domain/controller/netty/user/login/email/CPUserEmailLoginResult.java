package team.carrypigeon.backend.chat.domain.controller.netty.user.login.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户邮箱登录响应数据
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPUserEmailLoginResult {
    private String token;
}