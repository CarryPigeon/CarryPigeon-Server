package team.carrypigeon.backend.api.domain.bo.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPMessageData {
    private int type;
    private JsonNode data;
}
