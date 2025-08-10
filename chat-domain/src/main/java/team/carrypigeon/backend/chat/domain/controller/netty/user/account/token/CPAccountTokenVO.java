package team.carrypigeon.backend.chat.domain.controller.netty.user.account.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPAccountTokenVO {
    private String email;
    private String password;
    private int code;
    private String deviceName;
}
