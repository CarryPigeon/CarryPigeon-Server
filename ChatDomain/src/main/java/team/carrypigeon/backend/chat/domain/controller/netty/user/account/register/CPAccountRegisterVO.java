package team.carrypigeon.backend.chat.domain.controller.netty.user.account.register;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPAccountRegisterVO {
    private String email;
    private int code;
    private String name;
    private String password;
}
