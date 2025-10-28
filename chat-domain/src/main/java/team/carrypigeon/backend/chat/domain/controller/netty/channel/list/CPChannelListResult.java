package team.carrypigeon.backend.chat.domain.controller.netty.channel.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPChannelListResult {
    private int count;
    private CPChannelListResultItem[] channels;
}
