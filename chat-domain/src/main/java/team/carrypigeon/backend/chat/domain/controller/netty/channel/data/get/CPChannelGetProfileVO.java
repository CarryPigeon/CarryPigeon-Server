package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取频道信息请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelGetProfileVO {
    private long cid;
}
