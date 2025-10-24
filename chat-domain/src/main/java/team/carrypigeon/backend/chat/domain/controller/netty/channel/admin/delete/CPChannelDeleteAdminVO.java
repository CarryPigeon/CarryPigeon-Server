package team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.delete;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建频道管理员的请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelDeleteAdminVO {
    private long cid;
    private long uid;
}
