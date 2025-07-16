package team.carrypigeon.backend.connectionpool.heart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class HeartBeatMessage {
    @JsonIgnore
    public static final HeartBeatMessage INSTANCE = new HeartBeatMessage();
    private long id = -1;
    private String route = "HeartBeat";
}