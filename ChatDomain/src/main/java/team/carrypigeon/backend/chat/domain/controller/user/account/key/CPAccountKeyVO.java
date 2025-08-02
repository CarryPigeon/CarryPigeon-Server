package team.carrypigeon.backend.chat.domain.controller.user.account.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPAccountKeyVO {
    private String email;
    private String password;
}
