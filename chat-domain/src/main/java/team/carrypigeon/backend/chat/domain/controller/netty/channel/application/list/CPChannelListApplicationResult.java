package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取通道申请列表的返回结果
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
public class CPChannelListApplicationResult {
    private int count;
    private CPChannelListApplicationResultItem[] applications;
}
