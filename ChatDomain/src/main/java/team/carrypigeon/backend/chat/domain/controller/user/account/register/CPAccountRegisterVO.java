package team.carrypigeon.backend.chat.domain.controller.user.account.register;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPAccountRegisterVO {
    private String email;
    private String name;
    private String password;
}
