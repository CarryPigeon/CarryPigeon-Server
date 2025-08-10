package team.carrypigeon.backend.chat.domain.controller.netty.group.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天组获取返回数据包装类
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPGroupGetReturn {
    private long id;
    private long state;
}
