package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 获取频道信息请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelGetProfileResult {
    private String name;
    private long owner;
    private String brief;
    private long avatar;
    private long createTime;
}
