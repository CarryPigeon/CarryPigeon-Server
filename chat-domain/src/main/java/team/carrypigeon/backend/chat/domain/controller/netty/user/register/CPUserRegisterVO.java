package team.carrypigeon.backend.chat.domain.controller.netty.user.register;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求数据
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserRegisterVO {
    private String email;
    private int code;
}
