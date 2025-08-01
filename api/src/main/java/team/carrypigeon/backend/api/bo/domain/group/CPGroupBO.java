package team.carrypigeon.backend.api.bo.domain.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 群组聊天结构
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPGroupBO {
    private long id;
    private String name;
    private long owner;
    private String introduction;
    private long profile;
    private long registerTime;
    private long stateId;
}
