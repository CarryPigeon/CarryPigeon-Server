package team.carrypigeon.backend.chat.domain.controller.netty.user.account.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPAccountEditVO {
    private String name;
    private long profile;
    private String introduction;
}
