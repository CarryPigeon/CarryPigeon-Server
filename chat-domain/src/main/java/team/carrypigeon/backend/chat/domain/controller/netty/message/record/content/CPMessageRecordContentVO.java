package team.carrypigeon.backend.chat.domain.controller.netty.message.record.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPMessageRecordContentVO {
    private long[] mids;
}
