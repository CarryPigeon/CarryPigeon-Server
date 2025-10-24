package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.process;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通道处理申请的请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelProcessApplicationVO {
    private long aid;
    private int result;
}