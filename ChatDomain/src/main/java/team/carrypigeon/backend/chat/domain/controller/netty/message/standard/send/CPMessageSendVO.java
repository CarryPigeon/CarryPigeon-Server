package team.carrypigeon.backend.chat.domain.controller.netty.message.standard.send;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageSendVO {
    private Long toId;
    private String domain;
    private Integer type;
    private JsonNode data;
    @JsonSetter(nulls = Nulls.SKIP)
    private String route = "/core/msg/send";
}
