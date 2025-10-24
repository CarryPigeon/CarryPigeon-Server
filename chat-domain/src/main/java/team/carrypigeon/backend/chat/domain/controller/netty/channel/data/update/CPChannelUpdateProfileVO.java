package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新通道数据的参数类<br/>
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelUpdateProfileVO {
    private long cid;
    private String name;
    private long owner;
    private String brief;
    private long avatar;
}
