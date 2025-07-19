package team.carrypigeon.backend.chat.domain.controller.message.send;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPMessageSendVO {
    private long toId;
    private String content;
}
