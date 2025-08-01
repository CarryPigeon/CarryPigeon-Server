package team.carrypigeon.backend.chat.domain.controller.group.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPGroupUpdateVO {
    private long gid;
    private String name;
    private String introduction;
    private long profile;
}
