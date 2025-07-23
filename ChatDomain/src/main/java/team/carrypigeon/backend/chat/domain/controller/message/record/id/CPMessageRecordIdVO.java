package team.carrypigeon.backend.chat.domain.controller.message.record.id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPMessageRecordIdVO {
    private long fromTime;
    private int count;
    private long channelId;
}
