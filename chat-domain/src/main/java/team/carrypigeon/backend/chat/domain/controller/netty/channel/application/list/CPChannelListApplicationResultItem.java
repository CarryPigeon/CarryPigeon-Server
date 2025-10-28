package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 通道申请列表项
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannelListApplicationResultItem {
    // 申请id
    private long id;
    // 申请人id
    private long uid;
    // 申请状态
    private int state;
    // 申请消息
    private String msg;
    // 申请时间
    private long applyTime;
}