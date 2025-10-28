package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取消息列表结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageListResult {
    private int count;
    private CPMessageListResultItem[] messages;
}