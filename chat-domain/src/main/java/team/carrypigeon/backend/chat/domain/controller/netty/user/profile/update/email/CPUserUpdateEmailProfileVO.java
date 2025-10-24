package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPUserUpdateEmailProfileVO {
    private String newEmail;
    private int code;
}
