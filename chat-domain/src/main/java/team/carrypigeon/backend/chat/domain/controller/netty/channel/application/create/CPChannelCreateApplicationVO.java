package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.create;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建频道的申请参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelCreateApplicationVO {
    private long cid;
    private String msg;
}
