package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageCreateVO {
    private String type;
    private long cid;
    private JsonNode data;
}
