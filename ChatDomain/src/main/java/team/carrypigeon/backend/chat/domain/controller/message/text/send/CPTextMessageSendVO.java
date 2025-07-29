package team.carrypigeon.backend.chat.domain.controller.message.text.send;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPTextMessageSendVO {
    private long toId;
    private String text;
}
