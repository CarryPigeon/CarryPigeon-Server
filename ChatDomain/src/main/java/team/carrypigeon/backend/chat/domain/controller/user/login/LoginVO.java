package team.carrypigeon.backend.chat.domain.controller.user.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录值对象，用于前后端交互
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {
    private String email;
    private String password;
    private String deviceName;
}
