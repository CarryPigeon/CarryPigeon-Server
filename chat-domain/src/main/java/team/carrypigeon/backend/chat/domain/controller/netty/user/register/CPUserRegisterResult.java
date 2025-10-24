package team.carrypigeon.backend.chat.domain.controller.netty.user.register;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户注册响应数据
 * */
@Data
@AllArgsConstructor
public class CPUserRegisterResult {
    private String token;
}
