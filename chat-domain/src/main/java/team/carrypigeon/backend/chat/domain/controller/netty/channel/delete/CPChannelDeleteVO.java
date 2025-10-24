package team.carrypigeon.backend.chat.domain.controller.netty.channel.delete;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除通道的请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelDeleteVO {
    private long cid;
}
